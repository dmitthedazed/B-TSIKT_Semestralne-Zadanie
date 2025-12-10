package com.combined.backend.service;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;
    private final WebSocketNotificationService webSocketNotificationService;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository,
                       MembershipRepository membershipRepository, TaskRepository taskRepository,
                       ActivityLogService activityLogService,
                       WebSocketNotificationService webSocketNotificationService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.taskRepository = taskRepository;
        this.activityLogService = activityLogService;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Transactional
    public GroupDto createGroup(GroupDto groupDto) {
        User createdBy = userRepository.findById(groupDto.getCreatedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = new Group();
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());
        group.setCreatedBy(createdBy);
        group.setCreatedAt(LocalDateTime.now());

        Group savedGroup = groupRepository.save(group);

        // Automatically make the creator a member
        Membership membership = new Membership();
        membership.setUser(createdBy);
        membership.setGroup(savedGroup);
        membership.setRole(Membership.Role.ADMIN); // Creator is an admin
        membership.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        activityLogService.logActivity(createdBy.getUserId(), savedGroup.getGroupId(), "GROUP_CREATED", "Group '" + savedGroup.getName() + "' created.");
        activityLogService.logActivity(createdBy.getUserId(), savedGroup.getGroupId(), "USER_JOINED_GROUP", "User '" + createdBy.getNickname() + "' joined group '" + savedGroup.getName() + "' as ADMIN.");


        return toDto(savedGroup, true); // Creator is always a member
    }

    public List<GroupDto> getAllGroups(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        return groupRepository.findAll().stream()
                .map(group -> toDto(group, membershipRepository.existsByUserAndGroup(currentUser, group)))
                .collect(Collectors.toList());
    }

    public List<GroupDto> getMyGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return membershipRepository.findByUser(user).stream()
                .map(Membership::getGroup)
                .map(group -> toDto(group, true)) // User is definitely a member
                .collect(Collectors.toList());
    }

    public List<GroupDto> getAvailableGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Group> myGroups = membershipRepository.findByUser(user).stream()
                .map(Membership::getGroup)
                .collect(Collectors.toList());

        return groupRepository.findAll().stream()
                .filter(group -> !myGroups.contains(group))
                .map(group -> toDto(group, false)) // User is definitely not a member
                .collect(Collectors.toList());
    }

    public GroupDto getGroupById(Long id, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return toDto(group, membershipRepository.existsByUserAndGroup(currentUser, group));
    }

    private GroupDto toDto(Group group, boolean isMember) {
        long memberCount = membershipRepository.countByGroup(group);
        // Align with frontend statuses: consider OPEN and IN_PROGRESS as "open"
        long openTaskCount = taskRepository.countByGroupAndStatusIn(
            group,
            List.of("OPEN", "IN_PROGRESS")
        );

        return new GroupDto(
                group.getGroupId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedBy().getUserId(),
                group.getCreatedBy().getNickname(),
                group.getCreatedAt(),
                memberCount,
                openTaskCount,
                isMember
        );
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if user is already a member
        if (membershipRepository.existsByUserAndGroup(user, group)) {
            throw new RuntimeException("User is already a member of this group");
        }

        // Create membership
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(Membership.Role.MEMBER);
        membership.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        // Log activity
        activityLogService.logActivity(
            userId,
            groupId,
            "USER_JOINED_GROUP",
            "User '" + user.getNickname() + "' joined group '" + group.getName() + "'"
        );

        // Send WebSocket notification
        webSocketNotificationService.notifyUserJoined(groupId, user.getNickname(), group.getName());
    }
}
