package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.dto.ResourceDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OverviewController implements BaseController {

    @FXML private Label groupNameLabel;
    @FXML private Label groupDescriptionLabel;
    @FXML private Label createdByLabel;
    @FXML private Label membersCountLabel;
    @FXML private GridPane taskStatusGrid;
    @FXML private ListView<String> upcomingDeadlinesList;
    @FXML private ListView<String> materialsList;
    @FXML private ListView<String> memberLoadList;
    @FXML private Label statusLabel;
    @FXML private Label openCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label doneCountLabel;
    @FXML private StatsController statsSectionController;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentGroup;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
        if (statsSectionController != null) statsSectionController.setApiService(apiService);
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        if (statsSectionController != null) statsSectionController.setCurrentUser(userId, nickname);
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        if (statsSectionController != null) statsSectionController.setMainController(mainController);
    }

    public void setGroup(GroupDto group) {
        this.currentGroup = group;
        if (statsSectionController != null) statsSectionController.setGroup(group);
        if (group != null) {
            loadOverviewData();
        }
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    private void loadOverviewData() {
        if (apiService == null || currentGroup == null) {
            statusLabel.setText("API Service or Group not set.");
            return;
        }

        groupNameLabel.setText(currentGroup.getName());
        groupDescriptionLabel.setText(currentGroup.getDescription());
        createdByLabel.setText(currentGroup.getCreatedByName());
        membersCountLabel.setText(String.valueOf(currentGroup.getMemberCount()));

        statusLabel.setText("Loading overview data...");

        new Thread(() -> {
            try {
                // Fetch tasks for upcoming deadlines
                List<TaskDto> tasks = apiService.getTasksByGroup(currentGroup.getId());
                List<String> upcoming = tasks.stream()
                        .filter(task -> task.getDeadline() != null && task.getDeadline().isAfter(LocalDateTime.now()))
                        .sorted(Comparator.comparing(TaskDto::getDeadline))
                        .limit(5)
                        .map(task -> task.getTitle() + " (Due: " + task.getDeadline().toLocalDate() + ")")
                        .collect(Collectors.toList());

                // Fetch group statistics
                GroupStatsDto stats = apiService.getGroupStats(currentGroup.getId());

                // Fetch resources
                List<ResourceDto> resources = apiService.getResourcesByGroup(currentGroup.getId());

                Platform.runLater(() -> {
                    upcomingDeadlinesList.getItems().setAll(upcoming);
                    displayTaskStatusCounts(stats != null ? stats.getTaskStatusCounts() : null);
                    displayMemberLoad(stats != null ? stats.getTasksPerMember() : null);
                    displayResources(resources);
                    if (statsSectionController != null && stats != null) {
                        statsSectionController.setStats(stats);
                    }
                    statusLabel.setText("Overview data loaded.");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading overview data: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayTaskStatusCounts(Map<String, Long> statusCounts) {
        taskStatusGrid.getChildren().clear();
        long open = 0, inProgress = 0, done = 0;
        int row = 0;
        if (statusCounts != null) {
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toUpperCase();
                long val = entry.getValue() == null ? 0 : entry.getValue();
                if (key.contains("OPEN")) open += val;
                else if (key.contains("PROGRESS")) inProgress += val;
                else if (key.contains("DONE") || key.contains("COMPLETED")) done += val;

                Label status = new Label(entry.getKey() + ":");
                Label count = new Label(String.valueOf(val));
                taskStatusGrid.add(status, 0, row);
                taskStatusGrid.add(count, 1, row);
                row++;
            }
        }
        if (openCountLabel != null) openCountLabel.setText("Open: " + open);
        if (inProgressCountLabel != null) inProgressCountLabel.setText("In Progress: " + inProgress);
        if (doneCountLabel != null) doneCountLabel.setText("Done: " + done);
    }

    private void displayMemberLoad(Map<String, Long> memberLoad) {
        if (memberLoadList == null) return;
        memberLoadList.getItems().clear();
        if (memberLoad == null || memberLoad.isEmpty()) return;
        memberLoad.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(e -> memberLoadList.getItems().add(e.getKey() + ": " + e.getValue() + " tasks"));
    }

    private void displayResources(List<ResourceDto> resources) {
        if (materialsList == null) return;
        materialsList.getItems().clear();
        if (resources == null || resources.isEmpty()) return;
        resources.stream()
                .limit(8)
                .forEach(r -> {
                    String type = r.getType() == null ? "" : r.getType().toUpperCase();
                    String uploader = r.getUploadedByName() == null ? "" : (" · " + r.getUploadedByName());
                    materialsList.getItems().add(r.getTitle() + " (" + type + ")" + uploader);
                });
    }
}
