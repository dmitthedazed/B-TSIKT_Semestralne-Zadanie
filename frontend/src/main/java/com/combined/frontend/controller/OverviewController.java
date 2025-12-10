package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.GroupStatsDto;
import com.combined.backend.dto.TaskDto;
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
    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentGroup;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setGroup(GroupDto group) {
        this.currentGroup = group;
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
                        .map(task -> task.getTitle() + " (Due: " + task.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")")
                        .collect(Collectors.toList());

                // Fetch group statistics
                GroupStatsDto stats = apiService.getGroupStats(currentGroup.getId());

                Platform.runLater(() -> {
                    upcomingDeadlinesList.getItems().setAll(upcoming);
                    displayTaskStatusCounts(stats.getTaskStatusCounts());
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
        int row = 0;
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            Label status = new Label(entry.getKey() + ":");
            Label count = new Label(String.valueOf(entry.getValue()));
            taskStatusGrid.add(status, 0, row);
            taskStatusGrid.add(count, 1, row);
            row++;
        }
    }
}
