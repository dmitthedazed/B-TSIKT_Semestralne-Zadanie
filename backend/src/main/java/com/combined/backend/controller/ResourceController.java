package com.combined.backend.controller;

import com.combined.backend.dto.ResourceDto;
import com.combined.backend.service.ResourceService;
import com.combined.backend.config.CustomUserDetails;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceDto resourceDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(resourceService.createResource(userDetails.getUserId(), resourceDto));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResourceDto>> getResourcesByGroup(@PathVariable Long groupId,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(resourceService.getResourcesByGroupId(userDetails.getUserId(), groupId));
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable Long resourceId,
                                                      @RequestBody ResourceDto resourceDto,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(resourceService.updateResource(userDetails.getUserId(), resourceId, resourceDto));
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        resourceService.deleteResource(resourceId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{resourceId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long resourceId,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {
        var resource = resourceService.getResourceEntityWithAccess(resourceId, userDetails.getUserId());
        if (resource.getType() == null || !resource.getType().equalsIgnoreCase("FILE")) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get(resource.getPathOrUrl());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        InputStream in = new FileInputStream(path.toFile());
        String fileName = path.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(Files.size(path))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResourceDto> uploadResource(@RequestParam Long groupId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String title,
                                                      @RequestParam(required = false) String description,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(resourceService.uploadFile(userDetails.getUserId(), groupId, file, title, description));
    }
}
