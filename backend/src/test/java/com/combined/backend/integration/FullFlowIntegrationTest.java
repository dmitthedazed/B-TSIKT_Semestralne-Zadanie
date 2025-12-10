package com.combined.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void fullUserGroupTaskFlow() throws Exception {
        // register + login userA
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"A\",\"lastName\":\"A\",\"nickname\":\"userA\",\"email\":\"a@example.com\",\"password\":\"secret12\",\"confirmPassword\":\"secret12\"}"))
                .andExpect(status().isOk());
        var loginA = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk()).andReturn();
        String tokenA = "Bearer " + mapper.readTree(loginA.getResponse().getContentAsString()).get("token").asText();

        // create group
        var groupRes = mvc.perform(post("/api/groups").header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"G1\",\"description\":\"Group desc\"}"))
                .andExpect(status().isOk()).andReturn();
        long groupId = mapper.readTree(groupRes.getResponse().getContentAsString()).get("id").asLong();

        // register + login userB
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"B\",\"lastName\":\"B\",\"nickname\":\"userB\",\"email\":\"b@example.com\",\"password\":\"secret12\",\"confirmPassword\":\"secret12\"}"))
                .andExpect(status().isOk());
        var loginB = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"b@example.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk()).andReturn();
        String tokenB = "Bearer " + mapper.readTree(loginB.getResponse().getContentAsString()).get("token").asText();

        // userB join
        mvc.perform(post("/api/memberships/join/" + groupId).header("Authorization", tokenB))
                .andExpect(status().isOk());

        // userA create task
        var taskRes = mvc.perform(post("/api/tasks").header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":" + groupId + ",\"title\":\"Task1\",\"status\":\"OPEN\"}"))
                .andExpect(status().isOk()).andReturn();
        long taskId = mapper.readTree(taskRes.getResponse().getContentAsString()).get("id").asLong();

        // userA delete task
        mvc.perform(delete("/api/tasks/" + taskId).header("Authorization", tokenA))
                .andExpect(status().isNoContent());
    }
}
