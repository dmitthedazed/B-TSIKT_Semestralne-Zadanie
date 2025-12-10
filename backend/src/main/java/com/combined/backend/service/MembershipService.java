package com.combined.backend.service;

import com.combined.backend.dto.MemberWithRoleDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import com.combined.backend.service.WebSocketNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ActivityLogService activityLogService; // New dependency
    private final TaskRepository taskRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public MembershipService(MembershipRepository membershipRepository,
                             UserRepository userRepository,
                             GroupRepository groupRepository,
                             ActivityLogService activityLogService,
                             TaskRepository taskRepository,
                             WebSocketNotificationService webSocketNotificationService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.activityLogService = activityLogService;
        this.taskRepository = taskRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Transactional
    public Membership joinGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (membershipRepository.existsByUserAndGroup(user, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(Membership.Role.MEMBER); // Default role
        membership.setJoinedAt(LocalDateTime.now());

        Membership savedMembership = membershipRepository.save(membership);
        activityLogService.logActivity(userId, groupId, "USER_JOINED_GROUP", "User '" + user.getNickname() + "' joined group '" + group.getName() + "'.");
        webSocketNotificationService.notifyMemberPayload(groupId, "MEMBER_JOINED", "Member joined: " + user.getNickname(), new MemberWithRoleDto(user, Membership.Role.MEMBER));
        return savedMembership;
    }

    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Membership membership = membershipRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        if (group.getCreatedBy().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Group creator cannot leave their own group");
        }

        membershipRepository.delete(membership);
        activityLogService.logActivity(userId, groupId, "USER_LEFT_GROUP", "User '" + user.getNickname() + "' left group '" + group.getName() + "'.");
        webSocketNotificationService.notifyMemberPayload(groupId, "MEMBER_LEFT", "Member left: " + user.getNickname(), new MemberWithRoleDto(user, Membership.Role.MEMBER));
    }

    public boolean isUserMemberOfGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return membershipRepository.existsByUserAndGroup(user, group);
    }

    public List<MemberWithRoleDto> getGroupMembers(Long groupId, Long requesterId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isCreator = group.getCreatedBy().getUserId().equals(requesterId);
        boolean isMember = membershipRepository.existsByUserAndGroup(requester, group);
        if (!isCreator && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can view members list");
        }
        List<MemberWithRoleDto> members = membershipRepository.findByGroup(group).stream()
                .map(m -> {
                    Membership.Role role = m.getUser().getUserId().equals(group.getCreatedBy().getUserId())
                            ? Membership.Role.CREATOR
                            : m.getRole();
                    return new MemberWithRoleDto(m.getUser(), role);
                })
                .collect(Collectors.toList());
        boolean creatorPresent = members.stream().anyMatch(m -> m.getUserId().equals(group.getCreatedBy().getUserId()));
        if (!creatorPresent) {
            members.add(new MemberWithRoleDto(group.getCreatedBy(), Membership.Role.CREATOR));
        }
        return members;
    }

    @Transactional
    public Membership addMember(Long actorId, Long groupId, Long userId, Membership.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        ensureActorIsAdminOrCreator(actorId, group);

        if (role == Membership.Role.CREATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign CREATOR role");
        }
        if (membershipRepository.existsByUserAndGroup(user, group)) {
            throw new RuntimeException("User is already a member of this group");
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(role);
        membership.setJoinedAt(LocalDateTime.now());

        Membership savedMembership = membershipRepository.save(membership);
        activityLogService.logActivity(userId, groupId, "MEMBER_ADDED", "User '" + user.getNickname() + "' added to group '" + group.getName() + "' as " + role.name() + ".");
        webSocketNotificationService.notifyMemberPayload(groupId, "MEMBER_ADDED", "Member added: " + user.getNickname(), new MemberWithRoleDto(user, role));
        return savedMembership;
    }

    @Transactional
    public Membership addMemberByEmail(Long actorId, Long groupId, String email, Membership.Role role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return addMember(actorId, groupId, user.getUserId(), role);
    }

    @Transactional
    public void removeMember(Long actorId, Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        ensureActorIsAdminOrCreator(actorId, group);

        if (group.getCreatedBy().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove group creator");
        }

        Membership membership = membershipRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        // Unassign tasks in this group that were assigned to the removed user
        taskRepository.findByGroupAndAssignedTo(group, user).forEach(task -> {
            task.setAssignedTo(null);
            taskRepository.save(task);
        });

        membershipRepository.delete(membership);
        activityLogService.logActivity(userId, groupId, "MEMBER_REMOVED", "User '" + user.getNickname() + "' removed from group '" + group.getName() + "'.");
        webSocketNotificationService.notifyMemberPayload(groupId, "MEMBER_REMOVED", "Member removed: " + user.getNickname(), new MemberWithRoleDto(user, membership.getRole()));
    }

    @Transactional
    public Membership updateMemberRole(Long actorId, Long groupId, Long userId, Membership.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        ensureActorIsAdminOrCreator(actorId, group);

        if (newRole == Membership.Role.CREATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign CREATOR role");
        }
        if (group.getCreatedBy().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change role of group creator");
        }

        Membership membership = membershipRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        Membership.Role oldRole = membership.getRole();
        membership.setRole(newRole);
        Membership updatedMembership = membershipRepository.save(membership);
        activityLogService.logActivity(userId, groupId, "MEMBER_ROLE_UPDATED", "User '" + user.getNickname() + "' role in group '" + group.getName() + "' changed from " + oldRole.name() + " to " + newRole.name() + ".");
        webSocketNotificationService.notifyMemberPayload(groupId, "MEMBER_ROLE_UPDATED", "Role updated for " + user.getNickname(), new MemberWithRoleDto(user, newRole));
        return updatedMembership;
    }

    private void ensureActorIsAdminOrCreator(Long actorId, Group group) {
        if (group.getCreatedBy().getUserId().equals(actorId)) {
            return;
        }
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("Actor not found"));
        boolean isAdmin = membershipRepository.findByUserAndGroup(actor, group)
                .map(m -> m.getRole() == Membership.Role.ADMIN)
                .orElse(false);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admin or creator can perform this action");
        }
    }
}
