package com.combined.backend.repository;

import com.combined.backend.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByGroup_GroupId(Long groupId); // Corrected method name
}
