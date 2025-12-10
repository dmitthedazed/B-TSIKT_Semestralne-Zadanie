package com.combined.backend.controller;

import com.combined.backend.config.CustomUserDetails;
import com.combined.backend.dto.GroupDto;
import com.combined.backend.service.GroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock GroupService groupService;
    @InjectMocks GroupController groupController;

    @Test
    void getMyGroupsReturnsList() {
        GroupDto dto = new GroupDto(1L, "G1", "Desc", 1L, "creator", LocalDateTime.now(), 1, true);
        when(groupService.getMyGroups(5L)).thenReturn(List.of(dto));
        CustomUserDetails cud = new CustomUserDetails(5L, "email", "pass", "nick");
        ResponseEntity<List<GroupDto>> res = groupController.getMyGroups(cud);
        assertThat(res.getBody()).hasSize(1);
    }
}
