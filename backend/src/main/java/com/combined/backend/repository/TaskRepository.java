package com.combined.backend.repository;

import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGroup(Group group);
    List<Task> findByAssignedTo(User user);
    List<Task> findByCreatedBy(User user);
    List<Task> findByGroupAndAssignedTo(Group group, User user);
    long countByGroupAndStatusIn(Group group, List<String> statuses);
}
