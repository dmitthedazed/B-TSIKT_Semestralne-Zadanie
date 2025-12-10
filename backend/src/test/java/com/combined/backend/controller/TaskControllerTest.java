package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock TaskService taskService;
    @InjectMocks TaskController taskController;

    @Test
    void getTasksForUserReturnsList() {
        TaskDto dto = new TaskDto(1L, 1L, "G", "Title", "D", "OPEN", null, 5L, "creator", null, null, LocalDateTime.now());
        when(taskService.getTasksForUser(any(), any(), any(), any(), any())).thenReturn(List.of(dto));
        CustomUserDetails cud = new CustomUserDetails(5L, "e", "p", "n");
        ResponseEntity<List<TaskDto>> res = taskController.getTasksForUser(cud, null, null, "ALL", null);
        assertThat(res.getBody()).hasSize(1);
    }
}
