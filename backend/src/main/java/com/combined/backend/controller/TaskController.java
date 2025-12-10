package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDto> createTask(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.createTask(userDetails.getUserId(), taskDto));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDto>> getTasksByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(taskService.getTasksByGroupId(groupId));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDto>> getTasksForUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long groupId,
                                                         @RequestParam(required = false, defaultValue = "ALL") String participation,
                                                         @RequestParam(required = false) String query) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(taskService.getTasksForUser(userId, status, groupId, participation, query));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDto> updateTask(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @PathVariable Long taskId,
                                              @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.updateTask(userDetails.getUserId(), taskId, taskDto));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @PathVariable Long taskId) {
        taskService.deleteTask(userDetails.getUserId(), taskId);
        return ResponseEntity.noContent().build();
    }
}
