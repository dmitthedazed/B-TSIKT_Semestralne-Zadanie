package com.combined.backend.service;

import com.combined.backend.dto.ActivityLogDto;
import com.combined.backend.model.ActivityLog;
import com.combined.backend.model.Group;
import com.combined.backend.model.User;
import com.combined.backend.repository.ActivityLogRepository;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

    import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock ActivityLogRepository activityLogRepository;
    @Mock UserRepository userRepository;
    @Mock GroupRepository groupRepository;
    @Mock MembershipRepository membershipRepository;

    @InjectMocks
    ActivityLogService activityLogService;

    @Test
    void filtersByDaysBack() {
        User u = new User(); u.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        ActivityLog old = new ActivityLog(u, null, "OLD", "old", LocalDateTime.now().minusDays(10));
        ActivityLog recent = new ActivityLog(u, null, "NEW", "new", LocalDateTime.now().minusDays(1));
        when(activityLogRepository.findByUser(u)).thenReturn(List.of(old, recent));
        List<ActivityLogDto> res = activityLogService.getUserActivityLogs(1L, 7);
        assertThat(res).extracting(ActivityLogDto::getActivityType).containsExactly("NEW");
    }

    @Test
    void groupLogsForbiddenForNonMember() {
        Group g = new Group(); g.setGroupId(1L);
        User u = new User(); u.setUserId(2L);
        User creator = new User(); creator.setUserId(3L); g.setCreatedBy(creator);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(g));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));
        when(membershipRepository.existsByUserAndGroup(u, g)).thenReturn(false);

        assertThatThrownBy(() -> activityLogService.getGroupActivityLogs(1L, 2L))
                .isInstanceOf(RuntimeException.class);
    }
}
