package com.combined.backend.websocket;

import com.combined.backend.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private WebSocketNotificationService service;

    @Test
    void notifyGroupSendsToTopic() {
        service.notifyGroup(1L, "TASK_CREATED", "created");
        verify(messagingTemplate).convertAndSend(eq("/topic/group/1"), org.mockito.ArgumentMatchers.<Object>any());
    }
}
