package com.combined.backend.controller;

import com.combined.backend.dto.ResourceDto;
import com.combined.backend.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceDto resourceDto) {
        return ResponseEntity.ok(resourceService.createResource(resourceDto));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResourceDto>> getResourcesByGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(resourceService.getResourcesByGroupId(groupId));
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable Long resourceId, @RequestBody ResourceDto resourceDto) {
        return ResponseEntity.ok(resourceService.updateResource(resourceId, resourceDto));
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId) {
        resourceService.deleteResource(resourceId);
        return ResponseEntity.noContent().build();
    }
}
