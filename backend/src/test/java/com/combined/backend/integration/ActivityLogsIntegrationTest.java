package com.combined.backend.integration;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.model.Group;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.UserRepository;
import com.combined.backend.service.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityLogsIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ActivityLogService activityLogService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Group group;
    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail("logger@example.com");
        user.setNickname("logger");
        user.setFirstName("Logger");
        user.setPasswordHash(passwordEncoder.encode("pass123"));
        user = userRepository.save(user);

        group = new Group();
        group.setName("Log Group");
        group.setDescription("Log group description");
        group.setCreatedAt(LocalDateTime.now());
        group.setCreatedBy(user);
        group = groupRepository.save(group);

        token = "Bearer " + jwtUtil.generateToken(user);

        activityLogService.logActivity(user.getUserId(), group.getGroupId(), "GROUP_CREATED", "Group created");
        activityLogService.logActivity(user.getUserId(), group.getGroupId(), "TASK_CREATED", "Task 1");
    }

    @Test
    void userCanFetchOwnLogs() throws Exception {
        mvc.perform(get("/api/activity-logs/user/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityType").value("TASK_CREATED"))
                .andExpect(jsonPath("$[1].activityType").value("GROUP_CREATED"));
    }

    @Test
    void groupLogsReturnedForMember() throws Exception {
        mvc.perform(get("/api/activity-logs/group/" + group.getGroupId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityType").value("TASK_CREATED"));
    }
}
