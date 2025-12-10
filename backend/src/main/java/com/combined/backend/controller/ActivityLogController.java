package com.combined.backend.controller;

import com.combined.backend.model.ActivityLog;
import com.combined.backend.dto.ActivityLogDto;
import com.combined.backend.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.combined.backend.config.CustomUserDetails;
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
    public ResponseEntity<List<ActivityLog>> getGroupActivityLogs(@PathVariable Long groupId,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(activityLogService.getGroupActivityLogs(groupId, userDetails.getUserId()));
    }

    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActivityLogDto>> getUserActivityLogs(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                    @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(activityLogService.getUserActivityLogs(userDetails.getUserId(), days));
    }
}
