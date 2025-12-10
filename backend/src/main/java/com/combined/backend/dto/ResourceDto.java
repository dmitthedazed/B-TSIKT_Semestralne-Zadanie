package com.combined.backend.dto;

import java.time.LocalDateTime;

public class ResourceDto {
    private Long id;
    private Long groupId;
    private String title;
    private String type; // e.g., "LINK", "FILE"
    private String pathOrUrl;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

    public ResourceDto() {
    }

    public ResourceDto(Long id, Long groupId, String title, String type, String pathOrUrl, Long uploadedById, String uploadedByName, LocalDateTime uploadedAt) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.type = type;
        this.pathOrUrl = pathOrUrl;
        this.uploadedById = uploadedById;
        this.uploadedByName = uploadedByName;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPathOrUrl() {
        return pathOrUrl;
    }

    public void setPathOrUrl(String pathOrUrl) {
        this.pathOrUrl = pathOrUrl;
    }

    public Long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(Long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
