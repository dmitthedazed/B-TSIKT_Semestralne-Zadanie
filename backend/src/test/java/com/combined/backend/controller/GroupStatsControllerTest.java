package com.combined.backend.controller;

import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.service.GroupStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupStatsControllerTest {

    @Mock GroupStatsService groupStatsService;
    @InjectMocks GroupStatsController groupStatsController;

    @Test
    void returnsStats() {
        GroupStatsDto dto = new GroupStatsDto(1L, Map.of("OPEN", 2L), Map.of("u1", 1L));
        when(groupStatsService.getGroupStats(1L)).thenReturn(dto);
        ResponseEntity<GroupStatsDto> res = groupStatsController.getGroupStats(1L);
        assertThat(res.getBody().getTaskStatusCounts()).containsEntry("OPEN", 2L);
    }
}
