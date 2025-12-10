package com.combined.backend.service;

import com.combined.backend.dto.ResourceDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Resource;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.ResourceRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public ResourceService(ResourceRepository resourceRepository, GroupRepository groupRepository, UserRepository userRepository, MembershipRepository membershipRepository) {
        this.resourceRepository = resourceRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ResourceDto createResource(ResourceDto resourceDto) {
        Group group = groupRepository.findById(resourceDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User uploadedBy = userRepository.findById(resourceDto.getUploadedById())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdminOrCreator(group, uploadedBy)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins/creator can add resources");
        }

        Resource resource = new Resource();
        resource.setGroup(group);
        resource.setUploadedBy(uploadedBy);
        resource.setTitle(resourceDto.getTitle());
        resource.setType(resourceDto.getType());
        resource.setPathOrUrl(resourceDto.getPathOrUrl());
        resource.setFileExtension(resourceDto.getFileExtension());
        resource.setDescription(resourceDto.getDescription());
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

        if (!canModifyResource(existingResource, resourceDto.getUploadedById())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this resource");
        }

        existingResource.setTitle(resourceDto.getTitle());
        existingResource.setType(resourceDto.getType());
        existingResource.setPathOrUrl(resourceDto.getPathOrUrl());
        existingResource.setFileExtension(resourceDto.getFileExtension());
        existingResource.setDescription(resourceDto.getDescription());

        Resource updatedResource = resourceRepository.save(existingResource);
        return toDto(updatedResource);
    }

    @Transactional
    public void deleteResource(Long resourceId, Long requesterId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!canModifyResource(resource, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this resource");
        }
        resourceRepository.delete(resource);
    }

    @Transactional
    public ResourceDto uploadFile(Long groupId, Long uploadedById, MultipartFile file, String title, String description) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User uploader = userRepository.findById(uploadedById)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!isAdminOrCreator(group, uploader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins/creator can upload files");
        }

        try {
            Path uploadDir = Paths.get("uploads", String.valueOf(groupId));
            Files.createDirectories(uploadDir);
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded";
            Path target = uploadDir.resolve(original);
            Files.copy(file.getInputStream(), target);

            String ext = "";
            int idx = original.lastIndexOf('.');
            if (idx > 0 && idx < original.length() - 1) {
                ext = original.substring(idx + 1);
            }

            ResourceDto dto = new ResourceDto();
            dto.setGroupId(groupId);
            dto.setUploadedById(uploadedById);
            dto.setTitle(title != null && !title.isBlank() ? title : original);
            dto.setType("FILE");
            dto.setPathOrUrl(target.toString());
            dto.setFileExtension(ext);
            dto.setDescription(description);
            return createResource(dto);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to store file: " + ex.getMessage(), ex);
        }
    }

    private ResourceDto toDto(Resource resource) {
        return new ResourceDto(
                resource.getResourceId(),
                resource.getGroup().getGroupId(),
                resource.getTitle(),
                resource.getType(),
                resource.getPathOrUrl(),
                resource.getFileExtension(),
                resource.getDescription(),
                resource.getUploadedBy().getUserId(),
                resource.getUploadedBy().getNickname(),
                resource.getUploadedAt()
        );
    }

    public Resource getResourceEntity(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }

    private boolean isAdminOrCreator(Group group, User user) {
        if (group.getCreatedBy().getUserId().equals(user.getUserId())) {
            return true;
        }
        return membershipRepository.findByUserAndGroup(user, group)
                .map(m -> m.getRole() == Membership.Role.ADMIN)
                .orElse(false);
    }

    private boolean canModifyResource(Resource resource, Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group group = resource.getGroup();
        if (isAdminOrCreator(group, requester)) {
            return true;
        }
        return resource.getUploadedBy().getUserId().equals(requesterId);
    }
}
