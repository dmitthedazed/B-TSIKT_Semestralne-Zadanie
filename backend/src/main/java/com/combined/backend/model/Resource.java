package com.combined.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long resourceId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Type cannot be blank")
    @Column(nullable = false)
    private String type; // e.g., "LINK", "FILE"

    @NotBlank(message = "Path or URL cannot be blank")
    @Column(name = "path_or_url", nullable = false)
    private String pathOrUrl;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public Resource() {
    }

    // Getters and Setters
    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
