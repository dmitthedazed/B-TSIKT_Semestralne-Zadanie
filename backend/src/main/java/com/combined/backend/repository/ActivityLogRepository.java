package com.combined.backend.repository;

import com.combined.backend.model.ActivityLog;
import com.combined.backend.model.Group;
import com.combined.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUser(User user);
    List<ActivityLog> findByGroup(Group group);
    List<ActivityLog> findByGroupOrderByTimestampDesc(Group group);
}
