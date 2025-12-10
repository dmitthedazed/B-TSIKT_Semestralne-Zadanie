package com.combined.backend.dto;

import java.time.LocalDateTime;

public class ActivityLogDto {
    private Long id;
    private Long userId;
    private Long groupId;
    private String groupName;
    private String activityType;
    private String description;
    private LocalDateTime timestamp;

    public ActivityLogDto() {
    }

    public ActivityLogDto(Long id, Long userId, Long groupId, String groupName,
                          String activityType, String description, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.activityType = activityType;
        this.description = description;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
