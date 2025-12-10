package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.GroupDto;
import com.combined.backend.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupDto> createGroup(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestBody GroupDto groupDto) {
        return ResponseEntity.ok(groupService.createGroup(userDetails.getUserId(), groupDto));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDto>> getMyGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(groupService.getMyGroups(userId));
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDto>> getAvailableGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(groupService.getAvailableGroups(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupDto> getGroupById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(groupService.getGroupById(id, userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupDto>> getAllGroups(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(groupService.getAllGroups(userId));
    }

    @PostMapping("/{groupId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> joinGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        groupService.joinGroup(groupId, userId);
        return ResponseEntity.ok("Successfully joined the group");
    }
}
