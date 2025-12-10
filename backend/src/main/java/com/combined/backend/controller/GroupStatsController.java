package com.combined.backend.controller;

import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.service.GroupStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group-stats")
public class GroupStatsController {

    private final GroupStatsService groupStatsService;

    public GroupStatsController(GroupStatsService groupStatsService) {
        this.groupStatsService = groupStatsService;
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupStatsDto> getGroupStats(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupStatsService.getGroupStats(groupId));
    }
}
