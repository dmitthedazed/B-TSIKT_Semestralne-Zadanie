package com.combined.frontend.service;

/**
 * Marker for controllers that need WebSocket notifications.
 */
public interface NotificationAware {
    void setNotificationService(NotificationService notificationService);
}
