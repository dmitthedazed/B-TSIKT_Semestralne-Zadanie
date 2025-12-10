package com.combined.backend.repository;

import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUser(User user);
    List<Membership> findByGroup(Group group);
    Optional<Membership> findByUserAndGroup(User user, Group group);
    boolean existsByUserAndGroup(User user, Group group);
    long countByGroup(Group group);
}
