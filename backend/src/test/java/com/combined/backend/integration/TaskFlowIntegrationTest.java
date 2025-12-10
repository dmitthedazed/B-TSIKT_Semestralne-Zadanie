package com.combined.backend.integration;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.model.Group;
import com.combined.backend.model.Task;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.TaskRepository;
import com.combined.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper mapper;

    private String creatorToken;
    private Group group;

    @BeforeEach
    void setUp() {
        User creator = new User();
        creator.setEmail("creator@example.com");
        creator.setNickname("creator");
        creator.setFirstName("Creator");
        creator.setPasswordHash(passwordEncoder.encode("pass123"));
        creator = userRepository.save(creator);

        group = new Group();
        group.setName("Task Group");
        group.setDescription("Task group description");
        group.setCreatedAt(LocalDateTime.now());
        group.setCreatedBy(creator);
        group = groupRepository.save(group);

        creatorToken = "Bearer " + jwtUtil.generateToken(creator);
    }

    @Test
    void createUpdateDeleteTaskHappyPath() throws Exception {
        String createBody = mapper.writeValueAsString(Map.of(
                "groupId", group.getGroupId(),
                "title", "Task 1",
                "description", "Task description",
                "status", "OPEN"
        ));

        var createRes = mvc.perform(post("/api/tasks")
                        .header("Authorization", creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task 1"))
                .andReturn();

        Long taskId = mapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        String updateBody = mapper.writeValueAsString(Map.of(
                "groupId", group.getGroupId(),
                "title", "T1 updated",
                "description", "Updated",
                "status", "DONE"
        ));

        mvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(get("/api/tasks/my")
                        .header("Authorization", creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("T1 updated"));

        mvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", creatorToken))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.findAll()).isEmpty();
    }
}
