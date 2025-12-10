package com.combined.backend.service;

import com.combined.backend.dto.TaskDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
    public TaskDto createTask(Long actorId, TaskDto taskDto) {
        Group group = groupRepository.findById(taskDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User createdBy = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only group creator or admins/members of the group can create tasks
        if (!isMemberOfGroup(actorId, group.getGroupId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group");
        }

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
    public TaskDto updateTask(Long actorId, Long taskId, TaskDto taskDto) {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!canModifyTask(actorId, existingTask)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify this task");
        }

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
    public void deleteTask(Long actorId, Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found");
        }
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!canManageTask(actorId, existingTask)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this task");
        }
        taskRepository.delete(existingTask);
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

    private boolean isMemberOfGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if (group.getCreatedBy() != null && group.getCreatedBy().getUserId().equals(userId)) {
            return true;
        }
        return membershipRepository.existsByUserAndGroup(user, group);
    }

    private boolean canModifyTask(Long actorId, Task task) {
        // Allow if actor is group creator, group admin, task creator, or current assignee
        if (task.getGroup().getCreatedBy().getUserId().equals(actorId)) return true;
        if (isGroupAdmin(actorId, task.getGroup())) return true;
        if (task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(actorId)) return true;
        return task.getAssignedTo() != null && task.getAssignedTo().getUserId().equals(actorId);
    }

    private boolean canManageTask(Long actorId, Task task) {
        // Stricter: only group creator/admin or task creator can delete
        if (task.getGroup().getCreatedBy().getUserId().equals(actorId)) return true;
        if (isGroupAdmin(actorId, task.getGroup())) return true;
        return task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(actorId);
    }

    private boolean isGroupAdmin(Long actorId, Group group) {
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return membershipRepository.findByUserAndGroup(user, group)
                .map(m -> m.getRole() == com.combined.backend.model.Membership.Role.ADMIN)
                .orElse(false);
    }
}
