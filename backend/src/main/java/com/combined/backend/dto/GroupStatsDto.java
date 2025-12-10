package com.combined.backend.dto;

import java.util.Map;

public class GroupStatsDto {
    private Long groupId;
    private Map<String, Long> taskStatusCounts; // e.g., {"OPEN": 5, "IN_PROGRESS": 3, "DONE": 10}
    private Map<String, Long> tasksPerMember; // e.g., {"user1": 7, "user2": 5}

    public GroupStatsDto() {
    }

    public GroupStatsDto(Long groupId, Map<String, Long> taskStatusCounts, Map<String, Long> tasksPerMember) {
        this.groupId = groupId;
        this.taskStatusCounts = taskStatusCounts;
        this.tasksPerMember = tasksPerMember;
    }

    // Getters and Setters
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Map<String, Long> getTaskStatusCounts() {
        return taskStatusCounts;
    }

    public void setTaskStatusCounts(Map<String, Long> taskStatusCounts) {
        this.taskStatusCounts = taskStatusCounts;
    }

    public Map<String, Long> getTasksPerMember() {
        return tasksPerMember;
    }

    public void setTasksPerMember(Map<String, Long> tasksPerMember) {
        this.tasksPerMember = tasksPerMember;
    }
}
