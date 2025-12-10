package com.combined.backend.service;

import com.combined.backend.dto.ResourceDto;
import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.Resource;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.ResourceRepository;
import com.combined.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceAccessTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private ResourceService resourceService;

    private User creator;
    private User member;
    private Group group;

    @BeforeEach
    void init() {
        creator = new User();
        creator.setUserId(1L);
        creator.setNickname("creator");
        member = new User();
        member.setUserId(2L);
        member.setNickname("member");

        group = new Group();
        group.setGroupId(10L);
        group.setName("G");
        group.setDescription("D");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void memberCannotCreateResource() {
        ResourceDto dto = new ResourceDto();
        dto.setGroupId(group.getGroupId());
        dto.setTitle("Doc");
        dto.setType("LINK");
        dto.setPathOrUrl("http://example.com");

        when(groupRepository.findById(group.getGroupId())).thenReturn(Optional.of(group));
        when(userRepository.findById(member.getUserId())).thenReturn(Optional.of(member));
        when(membershipRepository.findByUserAndGroup(member, group))
                .thenReturn(Optional.of(new Membership(member, group, Membership.Role.MEMBER, LocalDateTime.now())));

        assertThatThrownBy(() -> resourceService.createResource(member.getUserId(), dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(resourceRepository, never()).save(any(Resource.class));
    }

    @Test
    void creatorCanCreateResource() {
        ResourceDto dto = new ResourceDto();
        dto.setGroupId(group.getGroupId());
        dto.setTitle("Doc");
        dto.setType("LINK");
        dto.setPathOrUrl("http://example.com");

        when(groupRepository.findById(group.getGroupId())).thenReturn(Optional.of(group));
        when(userRepository.findById(creator.getUserId())).thenReturn(Optional.of(creator));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> {
            Resource r = inv.getArgument(0);
            r.setResourceId(99L);
            r.setUploadedAt(LocalDateTime.now());
            return r;
        });

        resourceService.createResource(creator.getUserId(), dto);

        verify(resourceRepository).save(any(Resource.class));
    }
}
