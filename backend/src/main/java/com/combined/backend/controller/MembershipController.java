package com.combined.backend.controller;

import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import com.combined.backend.dto.MemberWithRoleDto;
import com.combined.backend.service.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/join/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> joinGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        membershipService.joinGroup(userId, groupId);
        return ResponseEntity.ok("User joined group successfully");
    }

    @PostMapping("/leave/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> leaveGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        membershipService.leaveGroup(userId, groupId);
        return ResponseEntity.ok("User left group successfully");
    }

    @GetMapping("/group/{groupId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemberWithRoleDto>> getGroupMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(membershipService.getGroupMembers(groupId));
    }

    @PostMapping("/group/{groupId}/add-member")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Membership> addMember(@PathVariable Long groupId, @RequestParam Long userId, @RequestParam Membership.Role role) {
        return ResponseEntity.ok(membershipService.addMember(groupId, userId, role));
    }

    @PostMapping("/group/{groupId}/add-member-by-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Membership> addMemberByEmail(@PathVariable Long groupId, @RequestParam String email, @RequestParam(required = false, defaultValue = "MEMBER") Membership.Role role) {
        return ResponseEntity.ok(membershipService.addMemberByEmail(groupId, email, role));
    }

    @DeleteMapping("/group/{groupId}/remove-member")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @RequestParam Long userId) {
        membershipService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/group/{groupId}/update-role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Membership> updateMemberRole(@PathVariable Long groupId, @RequestParam Long userId, @RequestParam Membership.Role role) {
        return ResponseEntity.ok(membershipService.updateMemberRole(groupId, userId, role));
    }
}
