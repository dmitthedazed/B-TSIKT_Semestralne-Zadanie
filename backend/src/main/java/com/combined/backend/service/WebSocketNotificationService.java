package com.combined.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending WebSocket notifications to clients
 */
@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send notification to all members of a group
     * @param groupId Group ID
     * @param type Notification type (TASK_CREATED, TASK_UPDATED, etc.)
     * @param message Notification message
     */
    public void notifyGroup(Long groupId, String type, String message) {
        notifyGroup(groupId, type, message, null);
    }

    public void notifyGroup(Long groupId, String type, String message, Object payload) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("groupId", groupId);
        if (payload != null) {
            notification.put("payload", payload);
        }

        // Send to all subscribers of /topic/group/{groupId}
        messagingTemplate.convertAndSend("/topic/group/" + groupId, notification);

        System.out.println("WebSocket notification sent to group " + groupId + ": " + type);
    }

    /**
     * Send notification to a specific user
     * @param userId User ID
     * @param type Notification type
     * @param message Notification message
     */
    public void notifyUser(Long userId, String type, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        // Send to specific user's queue
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);

        System.out.println("WebSocket notification sent to user " + userId + ": " + type);
    }

    /**
     * Send task update notification
     */
    public void notifyTaskUpdate(Long groupId, String taskTitle, String action, String userName) {
        String message = userName + " " + action + " task: " + taskTitle;
        notifyGroup(groupId, "TASK_UPDATE", message);
    }

    public void notifyTaskPayload(Long groupId, String type, String message, Object payload) {
        notifyGroup(groupId, type, message, payload);
    }

    /**
     * Send resource uploaded notification
     */
    public void notifyResourceUploaded(Long groupId, String resourceTitle, String userName) {
        String message = userName + " uploaded: " + resourceTitle;
        notifyGroup(groupId, "RESOURCE_UPLOADED", message);
    }

    public void notifyResourcePayload(Long groupId, String type, String message, Object payload) {
        notifyGroup(groupId, type, message, payload);
    }

    /**
     * Send user joined notification
     */
    public void notifyUserJoined(Long groupId, String userName, String groupName) {
        String message = userName + " joined the group";
        notifyGroup(groupId, "USER_JOINED", message);
    }

    public void notifyMemberPayload(Long groupId, String type, String message, Object payload) {
        notifyGroup(groupId, type, message, payload);
    }
}

