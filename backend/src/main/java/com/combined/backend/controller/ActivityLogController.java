package com.combined.backend.controller;

import com.combined.backend.model.ActivityLog;
import com.combined.backend.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActivityLog>> getGroupActivityLogs(@PathVariable Long groupId) {
        return ResponseEntity.ok(activityLogService.getGroupActivityLogs(groupId));
    }
}
