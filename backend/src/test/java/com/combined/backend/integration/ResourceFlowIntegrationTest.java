package com.combined.backend.integration;

import com.combined.backend.config.JwtUtil;
import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.Resource;
import com.combined.backend.model.User;
import com.combined.backend.repository.GroupRepository;
import com.combined.backend.repository.MembershipRepository;
import com.combined.backend.repository.ResourceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResourceFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper mapper;

    private Group group;
    private String creatorToken;
    private String memberToken;

    @BeforeEach
    void init() {
        User creator = new User();
        creator.setEmail("creator2@example.com");
        creator.setNickname("creator2");
        creator.setFirstName("C2");
        creator.setPasswordHash(passwordEncoder.encode("pass123"));
        creator = userRepository.save(creator);

        User member = new User();
        member.setEmail("member2@example.com");
        member.setNickname("member2");
        member.setFirstName("M2");
        member.setPasswordHash(passwordEncoder.encode("pass123"));
        member = userRepository.save(member);

        group = new Group();
        group.setName("Resources Group");
        group.setDescription("Resources group description");
        group.setCreatedAt(LocalDateTime.now());
        group.setCreatedBy(creator);
        group = groupRepository.save(group);

        Membership creatorMembership = new Membership(creator, group, Membership.Role.CREATOR, LocalDateTime.now());
        Membership memberMembership = new Membership(member, group, Membership.Role.MEMBER, LocalDateTime.now());
        membershipRepository.save(creatorMembership);
        membershipRepository.save(memberMembership);

        creatorToken = "Bearer " + jwtUtil.generateToken(creator);
        memberToken = "Bearer " + jwtUtil.generateToken(member);
    }

    @Test
    void memberCanViewResourcesButCannotCreate() throws Exception {
        mvc.perform(post("/api/resources")
                        .header("Authorization", memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "groupId", group.getGroupId(),
                                "title", "Doc",
                                "type", "LINK",
                                "pathOrUrl", "http://example.com"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatorCreatesResourceAndMemberCanFetch() throws Exception {
        mvc.perform(post("/api/resources")
                        .header("Authorization", creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "groupId", group.getGroupId(),
                                "title", "Doc",
                                "type", "LINK",
                                "pathOrUrl", "http://example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Doc"));

        mvc.perform(get("/api/resources/group/" + group.getGroupId())
                        .header("Authorization", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Doc"));

        assertThat(resourceRepository.findAll()).hasSize(1);
    }
}
