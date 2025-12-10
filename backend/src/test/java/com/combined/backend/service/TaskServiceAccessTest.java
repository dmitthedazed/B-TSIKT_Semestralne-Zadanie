package com.combined.backend.service;

import com.combined.backend.dto.TaskDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lightweight access-control tests for TaskService without hitting the database.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceAccessTest {

    @Mock private TaskRepository taskRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private TaskService taskService;

    private User creator;
    private User outsider;
    private Group group;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUserId(42L);
        creator.setNickname("creator");

        outsider = new User();
        outsider.setUserId(7L);
        outsider.setNickname("outsider");

        group = new Group();
        group.setGroupId(100L);
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());
        group.setName("Test Group");
        group.setDescription("Desc");
    }

    @Test
    void createTaskFailsForNonMember() {
        TaskDto dto = new TaskDto();
        dto.setGroupId(group.getGroupId());
        dto.setTitle("Title");
        dto.setDescription("Desc");
        dto.setStatus("OPEN");

        when(groupRepository.findById(group.getGroupId())).thenReturn(Optional.of(group));
        when(userRepository.findById(outsider.getUserId())).thenReturn(Optional.of(outsider));
        when(membershipRepository.existsByUserAndGroup(outsider, group)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.createTask(outsider.getUserId(), dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTaskFailsWhenActorHasNoRights() {
        Task existing = new Task();
        existing.setTaskId(5L);
        existing.setGroup(group);
        existing.setCreatedBy(creator); // actor is not creator/admin/assignee
        existing.setTitle("Old");
        existing.setStatus("OPEN");
        existing.setCreatedAt(LocalDateTime.now());

        TaskDto dto = new TaskDto();
        dto.setGroupId(group.getGroupId());
        dto.setTitle("New title");
        dto.setDescription("New desc");
        dto.setStatus("DONE");

        when(taskRepository.findById(existing.getTaskId())).thenReturn(Optional.of(existing));
        when(userRepository.findById(outsider.getUserId())).thenReturn(Optional.of(outsider));
        when(membershipRepository.findByUserAndGroup(outsider, group)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> taskService.updateTask(outsider.getUserId(), existing.getTaskId(), dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(taskRepository, never()).save(any(Task.class));
    }
}
