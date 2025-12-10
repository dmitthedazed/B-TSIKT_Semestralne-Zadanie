package com.combined.backend.controller;

import com.combined.backend.dto.TaskDto;
import com.combined.backend.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<TaskDto> createTask(@RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.createTask(taskDto));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDto>> getTasksByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(taskService.getTasksByGroupId(groupId));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDto>> getTasksForUser(@RequestParam Long userId,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) Long groupId,
                                                         @RequestParam(required = false, defaultValue = "ALL") String participation,
                                                         @RequestParam(required = false) String query) {
        return ResponseEntity.ok(taskService.getTasksForUser(userId, status, groupId, participation, query));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long taskId, @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.updateTask(taskId, taskDto));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
