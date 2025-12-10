package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.Parent;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class GroupDetailController implements BaseController {

    @FXML private Label groupNameLabel;
    @FXML private Label groupDescriptionLabel;
    @FXML private Label groupInfoLabel;
    @FXML private TabPane groupTabPane;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentGroup; // The group being displayed
    private boolean tabsLoaded = false;

    // References to child controllers
    private OverviewController overviewController;
    private TasksController tasksController;
    private ResourcesController resourcesController;
    private MembersController membersController;
    private StatsController statsController;

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
            groupNameLabel.setText(group.getName());
            groupDescriptionLabel.setText(group.getDescription());
            groupInfoLabel.setText("Created by: " + group.getCreatedByName() + " on " + group.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            if (!tabsLoaded) {
                loadTabContent();
                tabsLoaded = true;
            } else {
                // update existing child controllers when switching groups
                if (overviewController != null) overviewController.setGroup(group);
                if (tasksController != null) tasksController.setCurrentlySelectedGroup(group);
                if (resourcesController != null) resourcesController.setCurrentlySelectedGroup(group);
                if (membersController != null) membersController.setGroup(group);
                if (statsController != null) statsController.setGroup(group);
            }
        }
    }

    @FXML
    private void initialize() {
        // Content will be loaded once setGroup is called
    }

    private void loadTabContent() {
        // Load Overview tab content
        Tab overviewTab = groupTabPane.getTabs().get(0);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/overview-view.fxml"));
            Parent overviewPane = loader.load();
            overviewController = loader.getController();
            overviewController.setApiService(apiService);
            overviewController.setCurrentUser(currentUserId, currentNickname);
            overviewController.setMainController(mainController);
            overviewController.setGroup(currentGroup);
            overviewTab.setContent(overviewPane);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load overview-view.fxml");
        }

        // Load Tasks tab content
        Tab tasksTab = groupTabPane.getTabs().get(1);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tasks-view.fxml"));
            Parent tasksPane = loader.load();
            tasksController = loader.getController();
            tasksController.setApiService(apiService);
            tasksController.setCurrentUser(currentUserId, currentNickname);
            tasksController.setMainController(mainController);
            tasksController.setCurrentlySelectedGroup(currentGroup); // Pass the group
            tasksTab.setContent(tasksPane);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load tasks-view.fxml");
        }

        // Load Materials tab content
        Tab resourcesTab = groupTabPane.getTabs().get(2);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resources-view.fxml"));
            Parent resourcesPane = loader.load();
            resourcesController = loader.getController();
            resourcesController.setApiService(apiService);
            resourcesController.setCurrentUser(currentUserId, currentNickname);
            resourcesController.setMainController(mainController);
            resourcesController.setCurrentlySelectedGroup(currentGroup); // Pass the group
            resourcesTab.setContent(resourcesPane);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load resources-view.fxml");
        }

        // Load Members tab content
        Tab membersTab = groupTabPane.getTabs().get(3);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/members-view.fxml"));
            Parent membersPane = loader.load();
            membersController = loader.getController();
            membersController.setApiService(apiService);
            membersController.setCurrentUser(currentUserId, currentNickname);
            membersController.setMainController(mainController);
            membersController.setGroup(currentGroup); // Pass the group
            membersTab.setContent(membersPane);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load members-view.fxml");
        }

        // Load Stats tab content
        Tab statsTab = groupTabPane.getTabs().get(4);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics-view.fxml"));
            Parent statsPane = loader.load();
            statsController = loader.getController();
            statsController.setApiService(apiService);
            statsController.setCurrentUser(currentUserId, currentNickname);
            statsController.setMainController(mainController);
            statsController.setGroup(currentGroup); // Pass the group
            statsTab.setContent(statsPane);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load statistics-view.fxml");
        }
    }
}
