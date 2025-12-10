package com.combined.backend.integration;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end happy-path checks for auth + group + tasks + memberships using H2 profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ObjectMapper mapper;

    private User creator;
    private User member;
    private Group group;
    private String creatorToken;
    private String memberToken;

    @BeforeEach
    void setupData() {
        creator = saveUser("alice@example.com", "Alice", "alice", "password123");
        member = saveUser("bob@example.com", "Bob", "bob", "password1234");

        group = new Group();
        group.setName("CS101");
        group.setDescription("Intro study group");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());
        group = groupRepository.save(group);

        Membership mCreator = new Membership(creator, group, Membership.Role.CREATOR, LocalDateTime.now());
        membershipRepository.save(mCreator);

        creatorToken = jwtUtil.generateToken(creator);
        memberToken = jwtUtil.generateToken(member);
    }

    @Test
    void authRegisterAndLoginWorks() throws Exception {
        String body = """
                {"firstName":"Test","lastName":"User","nickname":"tester","email":"tester@example.com",
                 "password":"secret12","confirmPassword":"secret12"}
                """;
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tester@example.com"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tester@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void memberCanJoinGroupAndCreatorCanRemove() throws Exception {
        mvc.perform(post("/api/memberships/join/" + group.getGroupId())
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());

        assertThat(membershipRepository.existsByUserAndGroup(member, group)).isTrue();

        mvc.perform(delete("/api/memberships/group/" + group.getGroupId() + "/remove-member")
                        .param("userId", member.getUserId().toString())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isNoContent());

        assertThat(membershipRepository.existsByUserAndGroup(member, group)).isFalse();
    }

    @Test
    void creatorCanCreateTaskAndSeeItInMyTasks() throws Exception {
        String taskBody = mapper.writeValueAsString(Map.of(
                "groupId", group.getGroupId(),
                "title", "Read chapter 1",
                "description", "Basics",
                "status", "OPEN"
        ));

        mvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(group.getGroupId()))
                .andExpect(jsonPath("$.title").value("Read chapter 1"));

        mvc.perform(get("/api/tasks/my")
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Read chapter 1"));

        assertThat(taskRepository.findAll()).hasSize(1);
    }

    @Test
    void nonMemberCannotDeleteCreatorCreatedGroupMember() throws Exception {
        // Bob joins the group first
        membershipRepository.save(new Membership(member, group, Membership.Role.MEMBER, LocalDateTime.now()));
        // Outsider without membership
        User outsider = saveUser("eve@example.com", "Eve", "eve", "password123");
        String outsiderToken = jwtUtil.generateToken(outsider);

        mvc.perform(delete("/api/memberships/group/" + group.getGroupId() + "/remove-member")
                        .param("userId", member.getUserId().toString())
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String email, String firstName, String nickname, String rawPassword) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setNickname(nickname);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
