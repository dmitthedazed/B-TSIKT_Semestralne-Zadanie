package com.combined.backend.service;

import com.combined.backend.dto.ActivityLogDto;
import com.combined.backend.model.ActivityLog;
import com.combined.backend.model.Group;
import com.combined.backend.model.User;
import com.combined.backend.repository.ActivityLogRepository;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.UserRepository;
import com.combined.backend.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              UserRepository userRepository,
                              GroupRepository groupRepository,
                              MembershipRepository membershipRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    public void logActivity(Long userId, Long groupId, String activityType, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for activity log"));
        Group group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found for activity log"));
        }

        ActivityLog log = new ActivityLog(user, group, activityType, description, LocalDateTime.now());
        activityLogRepository.save(log);
    }

    public List<ActivityLog> getGroupActivityLogs(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isCreator = group.getCreatedBy().getUserId().equals(userId);
        boolean isMember = membershipRepository.existsByUserAndGroup(user, group);
        if (!isCreator && !isMember) {
            throw new RuntimeException("User is not a member of this group");
        }

        return activityLogRepository.findByGroupOrderByTimestampDesc(group);
    }

    public List<ActivityLogDto> getUserActivityLogs(Long userId, Integer daysBack) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ActivityLog> logs = activityLogRepository.findByUser(user);
        LocalDateTime cutoff = null;
        if (daysBack != null && daysBack > 0) {
            cutoff = LocalDateTime.now().minusDays(daysBack);
        }
        LocalDateTime finalCutoff = cutoff;
        return logs.stream()
                .filter(log -> finalCutoff == null || log.getTimestamp().isAfter(finalCutoff))
                .sorted(Comparator.comparing(ActivityLog::getTimestamp).reversed())
                .map(log -> new ActivityLogDto(
                        log.getId(),
                        log.getUser().getUserId(),
                        log.getGroup() != null ? log.getGroup().getGroupId() : null,
                        log.getGroup() != null ? log.getGroup().getName() : null,
                        log.getActivityType(),
                        log.getDescription(),
                        log.getTimestamp()
                ))
                .collect(Collectors.toList());
    }
}
