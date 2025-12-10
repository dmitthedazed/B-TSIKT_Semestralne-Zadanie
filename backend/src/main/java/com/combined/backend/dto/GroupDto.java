package com.combined.backend.dto;

import java.time.LocalDateTime;

public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private long memberCount;
    private long openTaskCount;
    private boolean isMember;

    public GroupDto() {
    }

    public GroupDto(Long id, String name, String description, Long createdById, String createdByName, LocalDateTime createdAt, long memberCount, boolean isMember) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
        this.openTaskCount = 0;
        this.isMember = isMember;
    }

    public GroupDto(Long id, String name, String description, Long createdById, String createdByName, LocalDateTime createdAt, long memberCount, long openTaskCount, boolean isMember) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
        this.openTaskCount = openTaskCount;
        this.isMember = isMember;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public long getOpenTaskCount() {
        return openTaskCount;
    }

    public void setOpenTaskCount(long openTaskCount) {
        this.openTaskCount = openTaskCount;
    }

    public boolean getIsMember() {
        return isMember;
    }

    public void setIsMember(boolean member) {
        isMember = member;
    }
}
