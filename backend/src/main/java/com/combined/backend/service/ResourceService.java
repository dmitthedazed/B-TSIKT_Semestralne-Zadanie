package com.combined.backend.service;

import com.combined.backend.dto.ResourceDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Resource;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.ResourceRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public ResourceService(ResourceRepository resourceRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ResourceDto createResource(ResourceDto resourceDto) {
        Group group = groupRepository.findById(resourceDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User uploadedBy = userRepository.findById(resourceDto.getUploadedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resource resource = new Resource();
        resource.setGroup(group);
        resource.setUploadedBy(uploadedBy);
        resource.setTitle(resourceDto.getTitle());
        resource.setType(resourceDto.getType());
        resource.setPathOrUrl(resourceDto.getPathOrUrl());
        resource.setUploadedAt(LocalDateTime.now());

        Resource savedResource = resourceRepository.save(resource);
        return toDto(savedResource);
    }

    public List<ResourceDto> getResourcesByGroupId(Long groupId) {
        return resourceRepository.findByGroup_GroupId(groupId).stream() // Corrected method name
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResourceDto updateResource(Long resourceId, ResourceDto resourceDto) {
        Resource existingResource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        existingResource.setTitle(resourceDto.getTitle());
        existingResource.setType(resourceDto.getType());
        existingResource.setPathOrUrl(resourceDto.getPathOrUrl());

        Resource updatedResource = resourceRepository.save(existingResource);
        return toDto(updatedResource);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new RuntimeException("Resource not found");
        }
        resourceRepository.deleteById(resourceId);
    }

    private ResourceDto toDto(Resource resource) {
        return new ResourceDto(
                resource.getResourceId(),
                resource.getGroup().getGroupId(),
                resource.getTitle(),
                resource.getType(),
                resource.getPathOrUrl(),
                resource.getUploadedBy().getUserId(),
                resource.getUploadedBy().getNickname(),
                resource.getUploadedAt()
        );
    }
}
