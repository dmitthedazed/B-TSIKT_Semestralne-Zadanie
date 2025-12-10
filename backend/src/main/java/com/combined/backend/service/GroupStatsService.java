package com.combined.backend.service;

import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.model.Group;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupStatsService {

    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;

    public GroupStatsService(GroupRepository groupRepository, TaskRepository taskRepository) {
        this.groupRepository = groupRepository;
        this.taskRepository = taskRepository;
    }

    public GroupStatsDto getGroupStats(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Task Status Counts
        Map<String, Long> taskStatusCounts = taskRepository.findByGroup(group).stream()
                .collect(Collectors.groupingBy(com.combined.backend.model.Task::getStatus, Collectors.counting()));

        // Tasks Per Member (simplified: count tasks assigned to each member)
        Map<String, Long> tasksPerMember = taskRepository.findByGroup(group).stream()
                .filter(task -> task.getAssignedTo() != null)
                .collect(Collectors.groupingBy(task -> task.getAssignedTo().getNickname(), Collectors.counting()));

        return new GroupStatsDto(groupId, taskStatusCounts, tasksPerMember);
    }
}
