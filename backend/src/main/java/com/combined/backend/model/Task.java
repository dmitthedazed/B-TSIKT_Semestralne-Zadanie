package com.combined.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne // New field
    @JoinColumn(name = "assigned_to", nullable = true) // Can be null
    private User assignedTo;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    @Column(nullable = false)
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Status cannot be blank")
    @Size(min = 2, max = 20, message = "Status must be between 2 and 20 characters")
    @Column(nullable = false)
    private String status;

    private LocalDateTime deadline;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Task() {
    }

    public Task(Group group,
                User createdBy,
                User assignedTo, // New constructor parameter
                String title,
                String description,
                String status,
                LocalDateTime deadline,
                LocalDateTime createdAt) {
        this.group = group;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo; // Assign new field
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    // --- getters & setters ---

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) { // Corrected line
        this.createdBy = createdBy;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
