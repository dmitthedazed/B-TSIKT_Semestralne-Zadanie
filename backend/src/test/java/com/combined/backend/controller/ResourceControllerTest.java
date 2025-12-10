package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.ResourceDto;
import com.combined.backend.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock ResourceService resourceService;
    @InjectMocks ResourceController resourceController;

    @Test
    void getResourcesByGroupReturnsList() {
        ResourceDto dto = new ResourceDto(1L, 1L, "Doc", "LINK", "http://", null, null, 5L, "nick", LocalDateTime.now());
        when(resourceService.getResourcesByGroupId(any(), any())).thenReturn(List.of(dto));
        CustomUserDetails cud = new CustomUserDetails(5L, "e", "p", "n");
        ResponseEntity<List<ResourceDto>> res = resourceController.getResourcesByGroup(1L, cud);
        assertThat(res.getBody()).hasSize(1);
    }
}
