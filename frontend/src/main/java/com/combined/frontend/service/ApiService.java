package com.combined.frontend.service;

import com.combined.backend.dto.AuthRequest;
import com.combined.backend.dto.AuthResponse;
import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.dto.UserDto;
import com.combined.backend.dto.ResourceDto;
import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.dto.ActivityLogDto;
import com.combined.backend.dto.PasswordChangeDto;
import com.combined.backend.dto.ProfileUpdateDto;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class ApiService {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";
    // Allow overriding via system property/API_BASE_URL to hit demo (e.g., http://localhost:8081/api)
    private String baseUrl = System.getProperty("API_BASE_URL", DEFAULT_BASE_URL);
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private String token;
    private Long currentUserId; // Store the ID of the currently logged-in user
    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.baseUrl = baseUrl;
        }
    }
    public String getBaseUrl() {
        return baseUrl;
    }

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
                .uri(URI.create(baseUrl + "/auth/login"))
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
                .uri(URI.create(baseUrl + "/auth/register"))
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

    public List<GroupDto> getMyGroups() throws Exception {
        HttpRequest request = authenticatedGetRequest("/groups/my");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getMyGroups): Received status code: " + response.statusCode());
        System.out.println("ApiService (getMyGroups): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to get my groups");
        }
        throw new RuntimeException("Failed to get my groups: " + response.body());
    }

    public GroupDto getGroupById(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/groups/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), GroupDto.class);
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to load group");
        }
        throw new RuntimeException("Failed to load group: " + response.body());
    }

    public List<GroupDto> getAvailableGroups() throws Exception {
        HttpRequest request = authenticatedGetRequest("/groups/available");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getAvailableGroups): Received status code: " + response.statusCode());
        System.out.println("ApiService (getAvailableGroups): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to get available groups");
        }
        throw new RuntimeException("Failed to get available groups: " + response.body());
    }

    public GroupDto createGroup(GroupDto groupDto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(groupDto);
        HttpRequest request = authenticatedPostRequest("/groups", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (createGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), GroupDto.class);
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to create group");
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
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new RuntimeException("Unauthorized to join group");
            }
            throw new RuntimeException("Failed to join group: " + response.body());
        }
    }

    public List<GroupDto> getAllGroupsWithMembershipFlag() throws Exception {
        // Backend returns all groups with isMember flag
        HttpRequest request = authenticatedGetRequest("/groups");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getAllGroupsWithMembershipFlag): status " + response.statusCode());
        System.out.println("ApiService (getAllGroupsWithMembershipFlag): body " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<GroupDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to load groups");
        }
        throw new RuntimeException("Failed to load groups: " + response.body());
    }

    public void removeMember(Long groupId, Long userId) throws Exception {
        HttpRequest request = authenticatedDeleteRequest("/memberships/group/" + groupId + "/remove-member?userId=" + userId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (removeMember): Received status code: " + response.statusCode());
        System.out.println("ApiService (removeMember): Received response body: " + response.body());

        if (response.statusCode() == 204 || response.statusCode() == 200) return;
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to remove member");
        throw new RuntimeException("Failed to remove member: " + response.body());
    }

    public void updateMemberRole(Long groupId, Long userId, Membership.Role role) throws Exception {
        // For PUT, we need to send the role as a request parameter
        HttpRequest request = authenticatedPutRequest("/memberships/group/" + groupId + "/update-role?userId=" + userId + "&role=" + role.name(), "");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (updateMemberRole): Received status code: " + response.statusCode());
        System.out.println("ApiService (updateMemberRole): Received response body: " + response.body());

        if (response.statusCode() == 200) return;
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to update member role");
        throw new RuntimeException("Failed to update member role: " + response.body());
    }

    public void addMemberByEmail(Long groupId, String email, Membership.Role role) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String path = "/memberships/group/" + groupId + "/add-member-by-email?email=" + encodedEmail + "&role=" + role.name();
        HttpRequest request = authenticatedPostRequest(path, "");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (addMemberByEmail): Received status code: " + response.statusCode());
        System.out.println("ApiService (addMemberByEmail): Received response body: " + response.body());

        if (response.statusCode() == 200) return;
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to add member");
        throw new RuntimeException("Failed to add member: " + response.body());
    }

    public List<TaskDto> getTasksByGroup(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/tasks/group/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getTasksByGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (getTasksByGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<TaskDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to get tasks");
        }
        throw new RuntimeException("Failed to get tasks: " + response.body());
    }

    public List<TaskDto> getMyTasks(String status,
                                    Long groupId,
                                    String participation,
                                    String query) throws Exception {
        StringBuilder path = new StringBuilder("/tasks/my");
        if (status != null && !status.isBlank()) {
            path.append(path.toString().contains("?") ? "&" : "?").append("status=").append(encodeValue(status));
        }
        if (groupId != null) {
            path.append(path.toString().contains("?") ? "&" : "?").append("groupId=").append(groupId);
        }
        if (participation != null && !participation.isBlank()) {
            path.append(path.toString().contains("?") ? "&" : "?").append("participation=").append(encodeValue(participation));
        }
        if (query != null && !query.isBlank()) {
            path.append(path.toString().contains("?") ? "&" : "?").append("query=").append(encodeValue(query));
        }

        HttpRequest request = authenticatedGetRequest(path.toString());
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getMyTasks): Received status code: " + response.statusCode());
        System.out.println("ApiService (getMyTasks): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<TaskDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to fetch my tasks");
        }
        throw new RuntimeException("Failed to fetch my tasks: " + response.body());
    }

    public List<ActivityLogDto> getUserActivityLogs(Integer days) throws Exception {
        String path = "/activity-logs/user/me";
        if (days != null) {
            path += "?days=" + days;
        }
        HttpRequest request = authenticatedGetRequest(path);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("ApiService (getUserActivityLogs): status " + response.statusCode());
        System.out.println("ApiService (getUserActivityLogs): body " + response.body());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<ActivityLogDto>>() {});
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to load activity logs");
        }
        // Treat other non-200 as empty to keep UI responsive
        return List.of();
    }

    public UserDto getCurrentUser() throws Exception {
        HttpRequest request = authenticatedGetRequest("/users/me");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), UserDto.class);
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to load profile");
        }
        throw new RuntimeException("Failed to load profile: " + response.body());
    }

    public UserDto updateProfile(ProfileUpdateDto dto) throws Exception {
        String body = objectMapper.writeValueAsString(dto);
        HttpRequest request = authenticatedPutRequest("/users/me", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), UserDto.class);
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to update profile");
        }
        throw new RuntimeException("Failed to update profile: " + response.body());
    }

    public void changePassword(PasswordChangeDto dto) throws Exception {
        String body = objectMapper.writeValueAsString(dto);
        HttpRequest request = authenticatedPostRequest("/users/me/change-password", body);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) return;
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to change password");
        }
        throw new RuntimeException("Failed to change password: " + response.body());
    }

    public TaskDto createTask(TaskDto taskDto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(taskDto);
        HttpRequest request = authenticatedPostRequest("/tasks", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createTask): Received status code: " + response.statusCode());
        System.out.println("ApiService (createTask): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), TaskDto.class);
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to create task");
        }
        throw new RuntimeException("Failed to create task: " + response.body());
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
        }
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to update task");
        throw new RuntimeException("Failed to update task: " + response.body());
    }

    public void deleteTask(Long taskId) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot delete task.");
        }
        HttpRequest request = authenticatedDeleteRequest("/tasks/" + taskId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (deleteTask): Received status code: " + response.statusCode());
        System.out.println("ApiService (deleteTask): Received response body: " + response.body());

        if (response.statusCode() == 204 || response.statusCode() == 200) return; // 204 No Content is typical for DELETE
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to delete task");
        throw new RuntimeException("Failed to delete task: " + response.body());
    }

    public List<ResourceDto> getResourcesByGroup(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/resources/group/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getResourcesByGroup): Received status code: " + response.statusCode());
        System.out.println("ApiService (getResourcesByGroup): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<ResourceDto>>() {});
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to get resources");
        } else {
            throw new RuntimeException("Failed to get resources: " + response.body());
        }
    }

    public ResourceDto createResource(ResourceDto resourceDto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(resourceDto);
        HttpRequest request = authenticatedPostRequest("/resources", requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (createResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (createResource): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ResourceDto.class);
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to create resource");
        } else {
            throw new RuntimeException("Failed to create resource: " + response.body());
        }
    }

    public ResourceDto updateResource(Long resourceId, ResourceDto resourceDto) throws Exception {
        String requestBody = objectMapper.writeValueAsString(resourceDto);
        HttpRequest request = authenticatedPutRequest("/resources/" + resourceId, requestBody);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (updateResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (updateResource): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ResourceDto.class);
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to update resource");
        } else {
            throw new RuntimeException("Failed to update resource: " + response.body());
        }
    }

    public void deleteResource(Long resourceId) throws Exception {
        HttpRequest request = authenticatedDeleteRequest("/resources/" + resourceId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (deleteResource): Received status code: " + response.statusCode());
        System.out.println("ApiService (deleteResource): Received response body: " + response.body());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to delete resource");
        }
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
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to get group members");
        } else {
            throw new RuntimeException("Failed to get group members: " + response.body());
        }
    }

    /**
     * Upload a file as multipart to /api/resources/upload
     */
    public ResourceDto uploadFileResource(Long groupId, String title, String description, String filePath) throws Exception {
        if (currentUserId == null) {
            throw new IllegalStateException("User not logged in. Cannot upload resource.");
        }
        var boundary = "----Boundary" + System.currentTimeMillis();
        var byteArrays = new java.util.ArrayList<java.lang.String>();
        // helper to add simple form field
        java.util.function.BiConsumer<String, String> addField = (name, value) -> {
            if (value == null) value = "";
            byteArrays.add("--" + boundary + "\r\n");
            byteArrays.add("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
            byteArrays.add(value + "\r\n");
        };
        addField.accept("groupId", groupId.toString());
        if (title != null) addField.accept("title", title);
        if (description != null) addField.accept("description", description);

        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        byte[] fileBytes = java.nio.file.Files.readAllBytes(path);
        String fileName = path.getFileName().toString();
        byteArrays.add("--" + boundary + "\r\n");
        byteArrays.add("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
        byteArrays.add("Content-Type: application/octet-stream\r\n\r\n");
        var bodyParts = new java.util.ArrayList<java.net.http.HttpRequest.BodyPublisher>();
        byteArrays.forEach(s -> bodyParts.add(java.net.http.HttpRequest.BodyPublishers.ofString(s)));
        bodyParts.add(java.net.http.HttpRequest.BodyPublishers.ofByteArray(fileBytes));
        bodyParts.add(java.net.http.HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/resources/upload"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(java.net.http.HttpRequest.BodyPublishers.concat(bodyParts.toArray(new java.net.http.HttpRequest.BodyPublisher[0])))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("ApiService (uploadFileResource): status " + response.statusCode());
        System.out.println("ApiService (uploadFileResource): body " + response.body());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ResourceDto.class);
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to upload resource");
        } else {
            throw new RuntimeException("Failed to upload resource: " + response.body());
        }
    }

    /**
     * Download a protected resource (FILE) using auth header and save to temp file.
     */
    public Path downloadResourceToTemp(Long resourceId, String suggestedName) throws Exception {
        HttpRequest request = authenticatedGetRequest("/resources/" + resourceId + "/download");
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println("ApiService (downloadResource): status " + response.statusCode());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException("Unauthorized to download file");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download file: " + response.statusCode());
        }
        String prefix = "resource_" + resourceId + "_";
        String suffix = "";
        if (suggestedName != null && suggestedName.contains(".")) {
            int idx = suggestedName.lastIndexOf('.');
            suffix = suggestedName.substring(idx);
        }
        Path tempFile = Files.createTempFile(prefix, suffix.isEmpty() ? ".bin" : suffix);
        Files.write(tempFile, response.body());
        return tempFile;
    }

    public GroupStatsDto getGroupStats(Long groupId) throws Exception {
        HttpRequest request = authenticatedGetRequest("/group-stats/" + groupId);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("ApiService (getGroupStats): Received status code: " + response.statusCode());
        System.out.println("ApiService (getGroupStats): Received response body: " + response.body());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), GroupStatsDto.class);
        }
        if (isUnauthorized(response.statusCode())) throw new RuntimeException("Unauthorized to get group stats");
        throw new RuntimeException("Failed to get group stats: " + response.body());
    }

    private boolean isUnauthorized(int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }

    private HttpRequest authenticatedGetRequest(String path) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private HttpRequest authenticatedPostRequest(String path, String body) {
        if (token == null) {
            throw new IllegalStateException("Authentication token is missing.");
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
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
                .uri(URI.create(baseUrl + path))
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
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
