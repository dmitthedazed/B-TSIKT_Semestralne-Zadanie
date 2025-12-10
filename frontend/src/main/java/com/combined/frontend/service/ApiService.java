package com.combined.frontend.service;

import com.combined.backend.dto.AuthRequest;
import com.combined.backend.dto.AuthResponse;
import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.dto.UserDto;
import com.combined.backend.dto.ResourceDto;
import com.combined.backend.dto.GroupStatsDto;
import com.combined.frontend.dto.MemberWithRoleDto;
import com.combined.backend.model.Membership; // New import
import com.combined.backend.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private String token;
    private Long currentUserId; // Store the ID of the currently logged-in user

    public void setTokenAndUserId(String token, Long userId) {
        this.token = token;
        this.currentUserId = userId;
    }

    public String getToken() {
        return token;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public void login(String email, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(email);
        authRequest.setPassword(password);

        String requestBody = objectMapper.writeValueAsString(authRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (Login): Received status code: " + response.statusCode());
        System.out.println("ApiService (Login): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
            this.token = authResponse.getToken();
        } else {
            throw new RuntimeException("Failed to login: " + response.body());
        }
    }

    public UserDto register(UserDto userDto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(userDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (Register): Received status code: " + response.statusCode());
        System.out.println("ApiService (Register): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            UserDto registeredUser = objectMapper.readValue(response.body(), UserDto.class);
            return registeredUser;
        } else {
            throw new RuntimeException("Failed to register: " + response.body());
        }
    }

    public List<GroupDto> getMyGroups(Long userId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/groups/my?userId=" + userId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getMyGroups): Received status code: " + response.statusCode());
        System.out.println("ApiService (getMyGroups): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        } else {
            throw new RuntimeException("Failed to get my groups: " + response.body());
        }
    }

    public List<GroupDto> getAvailableGroups(Long userId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/groups/available?userId=" + userId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getAvailableGroups): Received status code: " + response.statusCode());
        System.out.println("ApiService (getAvailableGroups): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        } else {
            throw new RuntimeException("Failed to get available groups: " + response.body());
        }
    }

    public GroupDto createGroup(GroupDto groupDto) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot create group.");
        }
        groupDto.setCreatedById(currentUserId);

        String requestBody = objectMapper.writeValueAsString(groupDto);
        HttpRequest request = authenticatedPostRequest("/groups", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (createGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), GroupDto.class);
        } else {
            throw new RuntimeException("Failed to create group: " + response.body());
        }
    }

    public void joinGroup(Long groupId) throws Exception {
        HttpRequest request = authenticatedPostRequest("/groups/" + groupId + "/join", "");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (joinGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (joinGroup): Received response body: " + response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to join group: " + response.body());
        }
    }

    public List<GroupDto> getAllGroupsWithMembershipFlag(Long userId) throws Exception {
        // Backend returns all groups with isMember flag
        HttpRequest request = authenticatedGetRequest("/groups");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getAllGroupsWithMembershipFlag): status " + response.statusCode());
        System.out.println("ApiService (getAllGroupsWithMembershipFlag): body " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        } else {
            throw new RuntimeException("Failed to load groups: " + response.body());
        }
    }

    public void removeMember(Long groupId, Long userId) throws Exception {
        HttpRequest request = authenticatedDeleteRequest("/memberships/group/" + groupId + "/remove-member?userId=" + userId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (removeMember): Received status code: " + response.statusCode());
        System.out.println("ApiService (removeMember): Received response body: " + response.body());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("Failed to remove member: " + response.body());
        }
    }

    public void updateMemberRole(Long groupId, Long userId, Membership.Role role) throws Exception {
        // For PUT, we need to send the role as a request parameter
        HttpRequest request = authenticatedPutRequest("/memberships/group/" + groupId + "/update-role?userId=" + userId + "&role=" + role.name(), "");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (updateMemberRole): Received status code: " + response.statusCode());
        System.out.println("ApiService (updateMemberRole): Received response body: " + response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to update member role: " + response.body());
        }
    }

    public void addMemberByEmail(Long groupId, String email, Membership.Role role) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String path = "/memberships/group/" + groupId + "/add-member-by-email?email=" + encodedEmail + "&role=" + role.name();
        HttpRequest request = authenticatedPostRequest(path, "");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (addMemberByEmail): Received status code: " + response.statusCode());
        System.out.println("ApiService (addMemberByEmail): Received response body: " + response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to add member: " + response.body());
        }
    }

    public List<TaskDto> getTasksByGroup(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/tasks/group/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getTasksByGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (getTasksByGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<TaskDto>>() {});
        } else {
            throw new RuntimeException("Failed to get tasks: " + response.body());
        }
    }

    public TaskDto createTask(TaskDto taskDto) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot create task.");
        }
        taskDto.setCreatedById(currentUserId);

        String requestBody = objectMapper.writeValueAsString(taskDto);
        HttpRequest request = authenticatedPostRequest("/tasks", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createTask): Received status code: " + response.statusCode());
        System.out.println("ApiService (createTask): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), TaskDto.class);
        } else {
            throw new RuntimeException("Failed to create task: " + response.body());
        }
    }

    public TaskDto updateTask(Long taskId, TaskDto taskDto) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot update task.");
        }
        // taskDto.setCreatedById(currentUserId); // CreatedBy should not change on update

        String requestBody = objectMapper.writeValueAsString(taskDto);
        HttpRequest request = authenticatedPutRequest("/tasks/" + taskId, requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (updateTask): Received status code: " + response.statusCode());
        System.out.println("ApiService (updateTask): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), TaskDto.class);
        } else {
            throw new RuntimeException("Failed to update task: " + response.body());
        }
    }

    public void deleteTask(Long taskId) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot delete task.");
        }
        HttpRequest request = authenticatedDeleteRequest("/tasks/" + taskId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (deleteTask): Received status code: " + response.statusCode());
        System.out.println("ApiService (deleteTask): Received response body: " + response.body());

        if (response.statusCode() != 204 && response.statusCode() != 200) { // 204 No Content is typical for DELETE
            throw new RuntimeException("Failed to delete task: " + response.body());
        }
    }

    public List<ResourceDto> getResourcesByGroup(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/resources/group/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getResourcesByGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (getResourcesByGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<ResourceDto>>() {});
        } else {
            throw new RuntimeException("Failed to get resources: " + response.body());
        }
    }

    public ResourceDto createResource(ResourceDto resourceDto) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot create resource.");
        }
        resourceDto.setUploadedById(currentUserId);
        resourceDto.setType("LINK"); // Default type for now

        String requestBody = objectMapper.writeValueAsString(resourceDto);
        HttpRequest request = authenticatedPostRequest("/resources", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (createResource): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ResourceDto.class);
        } else {
            throw new RuntimeException("Failed to create resource: " + response.body());
        }
    }

    public ResourceDto updateResource(Long resourceId, ResourceDto resourceDto) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot update resource.");
        }
        // resourceDto.setUploadedById(currentUserId); // UploadedBy should not change on update

        String requestBody = objectMapper.writeValueAsString(resourceDto);
        HttpRequest request = authenticatedPutRequest("/resources/" + resourceId, requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (updateResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (updateResource): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ResourceDto.class);
        } else {
            throw new RuntimeException("Failed to update resource: " + response.body());
        }
    }

    public void deleteResource(Long resourceId) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot delete resource.");
        }
        HttpRequest request = authenticatedDeleteRequest("/resources/" + resourceId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (deleteResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (deleteResource): Received response body: " + response.body());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("Failed to delete resource: " + response.body());
        }
    }

    public List<MemberWithRoleDto> getGroupMembers(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/memberships/group/" + groupId + "/members");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getGroupMembers): Received status code: " + response.statusCode());
        System.out.println("ApiService (getGroupMembers): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<MemberWithRoleDto>>() {});
        } else {
            throw new RuntimeException("Failed to get group members: " + response.body());
        }
    }

    public GroupStatsDto getGroupStats(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/group-stats/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getGroupStats): Received status code: " + response.statusCode());
        System.out.println("ApiService (getGroupStats): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), GroupStatsDto.class);
        } else {
            throw new RuntimeException("Failed to get group stats: " + response.body());
        }
    }

    private HttpRequest authenticatedGetRequest(String path) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private HttpRequest authenticatedPostRequest(String path, String body) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest authenticatedPutRequest(String path, String body) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest authenticatedDeleteRequest(String path) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
    }
}
