package com.combined.frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Minimal STOMP-over-WebSocket client for JavaFX.
 * Subscribes to backend broker topics and dispatches parsed notifications.
 */
public class NotificationService {
    private static final String WS_URL = "ws://localhost:8080/ws";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Long, String> subscriptionIds = new ConcurrentHashMap<>();
    private final Map<Long, Consumer<NotificationMessage>> groupHandlers = new ConcurrentHashMap<>();
    private final Set<Consumer<NotificationMessage>> globalHandlers = ConcurrentHashMap.newKeySet();
    private final java.util.Deque<NotificationMessage> recent = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final AtomicInteger subCounter = new AtomicInteger(1);
    private volatile boolean connected = false;

    private StompSocket client;
    private Long currentUserId;

    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }

    public synchronized void connect() {
        try {
            if (client != null && client.isOpen()) {
                return;
            }
            connected = false;
            client = new StompSocket(new URI(WS_URL));
            client.connectBlocking();
            client.sendConnectFrame();
            // subscriptions are sent once we receive CONNECTED frame
        } catch (Exception e) {
            System.err.println("NotificationService: Failed to connect to WebSocket: " + e.getMessage());
        }
    }

    public synchronized void disconnect() {
        try {
            if (client != null) {
                client.closeBlocking();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void subscribeToGroup(Long groupId, Consumer<NotificationMessage> handler) {
        if (groupId == null) return;
        groupHandlers.put(groupId, handler);
        connect();
        // Avoid duplicate STOMP subscriptions for the same group
        String existingId = subscriptionIds.get(groupId);
        if (existingId != null) {
            return;
        }
        String id = "sub-" + subCounter.getAndIncrement();
        subscriptionIds.put(groupId, id);
        if (client != null && client.isOpen() && connected) {
            client.sendSubscribe("/topic/group/" + groupId, id);
            System.out.println("NotificationService: Subscribed to group " + groupId + " with id " + id);
        }
    }

    public void subscribeToGroup(Long groupId) {
        subscribeToGroup(groupId, msg -> {});
    }

    private void resubscribeAll() {
        if (client == null || !client.isOpen() || !connected) return;
        for (Long groupId : Set.copyOf(groupHandlers.keySet())) {
            String id = subscriptionIds.computeIfAbsent(groupId, g -> "sub-" + subCounter.getAndIncrement());
            client.sendSubscribe("/topic/group/" + groupId, id);
            System.out.println("NotificationService: Re-subscribed to group " + groupId + " with id " + id);
        }
    }

    public void unsubscribeFromGroup(Long groupId) {
        if (groupId == null) return;
        Consumer<NotificationMessage> removed = groupHandlers.remove(groupId);
        String subId = subscriptionIds.remove(groupId);
        if (client != null && client.isOpen() && subId != null) {
            client.sendUnsubscribe(subId);
        }
    }

    public void registerGlobalHandler(Consumer<NotificationMessage> handler) {
        if (handler != null) {
            globalHandlers.add(handler);
            // replay a small history so late listeners see recent events
            for (NotificationMessage msg : recent) {
                handler.accept(msg);
            }
        }
    }

    public void unregisterGlobalHandler(Consumer<NotificationMessage> handler) {
        if (handler != null) {
            globalHandlers.remove(handler);
        }
    }

    private void dispatch(StompFrame frame) {
        String destination = frame.getHeader("destination");
        if (destination != null && destination.startsWith("/topic/group/")) {
            try {
                String groupStr = destination.substring("/topic/group/".length());
                Long groupId = Long.parseLong(groupStr);
                if (frame.body != null && !frame.body.isBlank()) {
                    NotificationMessage message = mapper.readValue(frame.body, NotificationMessage.class);
                    remember(message);
                    Consumer<NotificationMessage> handler = groupHandlers.get(groupId);
                    if (handler != null) {
                        handler.accept(message);
                    }
                    globalHandlers.forEach(h -> h.accept(message));
                }
            } catch (Exception e) {
                System.err.println("NotificationService: Failed to dispatch group message: " + e.getMessage());
            }
        }
    }

    private void remember(NotificationMessage msg) {
        recent.addFirst(msg);
        while (recent.size() > 50) {
            recent.removeLast();
        }
    }

    private class StompSocket extends WebSocketClient {
        public StompSocket(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("NotificationService: WebSocket opened");
        }

        @Override
        public void onMessage(String message) {
            // STOMP frames can be concatenated; handle one frame at a time
            for (String raw : message.split("\u0000")) {
                if (raw.isBlank()) continue;
                StompFrame frame = StompFrame.parse(raw);
                if (frame == null) continue;
                if ("CONNECTED".equalsIgnoreCase(frame.command)) {
                    connected = true;
                    System.out.println("NotificationService: STOMP CONNECTED");
                    resubscribeAll();
                    continue;
                }
                if ("MESSAGE".equalsIgnoreCase(frame.command)) {
                    String dest = frame.getHeader("destination");
                    System.out.println("NotificationService: MESSAGE received for " + dest);
                    dispatch(frame);
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("NotificationService: WebSocket closed: " + reason);
            connected = false;
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("NotificationService: WebSocket error: " + ex.getMessage());
        }

        void sendConnectFrame() {
            String frame = "CONNECT\naccept-version:1.1,1.2\nheart-beat:0,0\n\n\u0000";
            send(frame);
        }

        void sendSubscribe(String destination, String id) {
            String frame = "SUBSCRIBE\nid:" + id + "\ndestination:" + destination + "\n\n\u0000";
            send(frame);
        }

        void sendUnsubscribe(String id) {
            String frame = "UNSUBSCRIBE\nid:" + id + "\n\n\u0000";
            send(frame);
        }
    }

    private static class StompFrame {
        final String command;
        final Map<String, String> headers;
        final String body;

        StompFrame(String command, Map<String, String> headers, String body) {
            this.command = command;
            this.headers = headers;
            this.body = body;
        }

        static StompFrame parse(String raw) {
            try {
                String normalized = raw.replace("\r", "");
                String[] parts = normalized.split("\n\n", 2);
                String headerPart = parts[0];
                String body = parts.length > 1 ? parts[1] : "";
                String[] lines = headerPart.split("\n");
                String command = lines[0].trim();
                Map<String, String> headers = new ConcurrentHashMap<>();
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i];
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        headers.put(line.substring(0, idx), line.substring(idx + 1));
                    }
                }
                return new StompFrame(command, headers, body);
            } catch (Exception e) {
                System.err.println("NotificationService: Failed to parse STOMP frame: " + e.getMessage());
                return null;
            }
        }

        String getHeader(String name) {
            return headers.get(name);
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotificationMessage {
        private String type;
        private String message;
        private Long groupId;
        private long timestamp;
        private JsonNode payload;

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public Long getGroupId() {
            return groupId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public JsonNode getPayload() {
            return payload;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setGroupId(Long groupId) {
            this.groupId = groupId;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public void setPayload(JsonNode payload) {
            this.payload = payload;
        }
    }
}
