package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.GroupStatsDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.Map;

public class StatsController implements BaseController {

    @FXML private PieChart taskStatusPieChart;
    @FXML private BarChart<String, Number> tasksPerMemberBarChart;
    @FXML private Label statusLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label openTasksLabel;
    @FXML private Label doneTasksLabel;

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
            loadGroupStats();
        }
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
        // Initialize charts if needed
        tasksPerMemberBarChart.setAnimated(false);
        tasksPerMemberBarChart.setLegendVisible(false);
    }

    private void loadGroupStats() {
        if (apiService == null || currentGroup == null) {
            statusLabel.setText("API Service or Group not set.");
            return;
        }

        statusLabel.setText("Loading group statistics...");

        new Thread(() -> {
            try {
                GroupStatsDto stats = apiService.getGroupStats(currentGroup.getId());

                Platform.runLater(() -> {
                    updateTaskStatusPieChart(stats.getTaskStatusCounts());
                    updateTasksPerMemberBarChart(stats.getTasksPerMember());
                    updateSummary(stats.getTaskStatusCounts());
                    statusLabel.setText("Group statistics loaded.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading group statistics: " + e.getMessage());
                });
            }
        }).start();
    }

    private void updateTaskStatusPieChart(Map<String, Long> taskStatusCounts) {
        taskStatusPieChart.getData().clear();
        taskStatusCounts.forEach((status, count) -> {
            taskStatusPieChart.getData().add(new PieChart.Data(status + " (" + count + ")", count));
        });
    }

    private void updateTasksPerMemberBarChart(Map<String, Long> tasksPerMember) {
        tasksPerMemberBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        tasksPerMember.forEach((member, count) -> {
            series.getData().add(new XYChart.Data<>(member, count));
        });
        tasksPerMemberBarChart.getData().add(series);
    }

    private void updateSummary(Map<String, Long> statusCounts) {
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long done = statusCounts.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase("DONE") || e.getKey().equalsIgnoreCase("COMPLETED"))
                .mapToLong(Map.Entry::getValue)
                .sum();
        long open = total - done;
        totalTasksLabel.setText(String.valueOf(total));
        openTasksLabel.setText(String.valueOf(open));
        doneTasksLabel.setText(String.valueOf(done));
    }
}
