package com.combined.backend.service;

import com.combined.backend.dto.TaskDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public TaskService(TaskRepository taskRepository,
                       GroupRepository groupRepository,
                       UserRepository userRepository,
                       MembershipRepository membershipRepository) {
        this.taskRepository = taskRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        Group group = groupRepository.findById(taskDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User createdBy = userRepository.findById(taskDto.getCreatedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User assignedTo = null;
        if (taskDto.getAssignedToId() != null) {
            assignedTo = userRepository.findById(taskDto.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
        }

        Task task = new Task();
        task.setGroup(group);
        task.setCreatedBy(createdBy);
        task.setAssignedTo(assignedTo);
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setStatus(taskDto.getStatus());
        task.setDeadline(taskDto.getDeadline());
        task.setCreatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);
        return toDto(savedTask);
    }

    public List<TaskDto> getTasksByGroupId(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return taskRepository.findByGroup(group).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto updateTask(Long taskId, TaskDto taskDto) {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User assignedTo = null;
        if (taskDto.getAssignedToId() != null) {
            assignedTo = userRepository.findById(taskDto.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
        }

        existingTask.setTitle(taskDto.getTitle());
        existingTask.setDescription(taskDto.getDescription());
        existingTask.setStatus(taskDto.getStatus());
        existingTask.setDeadline(taskDto.getDeadline());
        existingTask.setAssignedTo(assignedTo);

        Task updatedTask = taskRepository.save(existingTask);
        return toDto(updatedTask);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found");
        }
        taskRepository.deleteById(taskId);
    }

    public List<TaskDto> getTasksForUser(Long userId,
                                         String status,
                                         Long groupId,
                                         String participation,
                                         String query) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String participationNormalized = participation == null ? "ALL" : participation.toUpperCase();
        Set<Task> tasks = new LinkedHashSet<>();

        // Tasks where user is assigned
        if (!"CREATED".equals(participationNormalized)) {
            tasks.addAll(taskRepository.findByAssignedTo(user));
        }

        // Tasks created by user
        if (!"ASSIGNED".equals(participationNormalized)) {
            tasks.addAll(taskRepository.findByCreatedBy(user));
        }

        // Tasks from groups where user is a member
        membershipRepository.findByUser(user).forEach(membership -> {
            tasks.addAll(taskRepository.findByGroup(membership.getGroup()));
        });

        // Optional filters
        return tasks.stream()
                .filter(task -> status == null || status.isBlank() || task.getStatus().equalsIgnoreCase(status))
                .filter(task -> groupId == null || task.getGroup().getGroupId().equals(groupId))
                .filter(task -> {
                    if (query == null || query.isBlank()) return true;
                    String lower = query.toLowerCase();
                    return (task.getTitle() != null && task.getTitle().toLowerCase().contains(lower)) ||
                            (task.getDescription() != null && task.getDescription().toLowerCase().contains(lower));
                })
                .filter(task -> {
                    if ("ASSIGNED".equals(participationNormalized)) {
                        return task.getAssignedTo() != null && task.getAssignedTo().getUserId().equals(userId);
                    }
                    if ("CREATED".equals(participationNormalized)) {
                        return task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(userId);
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private TaskDto toDto(Task task) {
        String assignedToName = (task.getAssignedTo() != null) ? task.getAssignedTo().getNickname() : null;
        Long assignedToId = (task.getAssignedTo() != null) ? task.getAssignedTo().getUserId() : null;

        return new TaskDto(
                task.getTaskId(),
                task.getGroup().getGroupId(),
                task.getGroup().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDeadline(),
                task.getCreatedBy().getUserId(),
                task.getCreatedBy().getNickname(),
                assignedToId,
                assignedToName,
                task.getCreatedAt()
        );
    }
}
