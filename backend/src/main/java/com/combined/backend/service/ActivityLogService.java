package com.combined.backend.service;

import com.combined.backend.model.ActivityLog;
import com.combined.backend.model.Group;
import com.combined.backend.model.User;
import com.combined.backend.repository.ActivityLogRepository;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository, GroupRepository groupRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
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

    public List<ActivityLog> getGroupActivityLogs(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return activityLogRepository.findByGroupOrderByTimestampDesc(group);
    }
}
