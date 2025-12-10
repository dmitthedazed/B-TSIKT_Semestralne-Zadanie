package com.combined.backend.service;

import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupStatsServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private TaskRepository taskRepository;

    @InjectMocks
    private GroupStatsService groupStatsService;

    @Test
    void countsStatusesAndTasksPerMember() {
        Group g = new Group();
        g.setGroupId(1L);
        User u1 = new User(); u1.setUserId(10L); u1.setNickname("u1");
        User u2 = new User(); u2.setUserId(11L); u2.setNickname("u2");
        Task t1 = new Task(g, u1, u1, "A", "D", "OPEN", null, LocalDateTime.now());
        Task t2 = new Task(g, u1, u2, "B", "D", "DONE", null, LocalDateTime.now());
        Task t3 = new Task(g, u2, u2, "C", "D", "IN_PROGRESS", null, LocalDateTime.now());

        when(groupRepository.findById(1L)).thenReturn(Optional.of(g));
        when(taskRepository.findByGroup(g)).thenReturn(List.of(t1, t2, t3));

        GroupStatsDto dto = groupStatsService.getGroupStats(1L);
        assertThat(dto.getTaskStatusCounts()).containsEntry("OPEN", 1L).containsEntry("DONE", 1L);
        assertThat(dto.getTasksPerMember()).containsEntry("u1", 1L).containsEntry("u2", 2L);
    }
}
