package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.MainApp;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private Label sectionTitleLabel;
    @FXML private Label usernameLabel;
    @FXML private StackPane contentPane; // Central content area

    // Sidebar Buttons
    @FXML private Button myGroupsButton;
    @FXML private Button tasksAllButton;
    @FXML private Button statisticsButton;
    @FXML private Button profileButton;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private GroupDto selectedGroup; // To hold the currently selected group

    // References to child controllers (only for the currently loaded view)
    private BaseController currentLoadedController;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        usernameLabel.setText("Welcome, " + nickname + "!");
        System.out.println("MainController: Current user set to ID: " + userId + ", Nickname: " + nickname);
        // Load default view (e.g., My Groups) after user is set
        loadView("/fxml/groups-view.fxml", "My Groups");
    }

    // Method to update the selected group and notify other controllers
    public void updateSelectedGroup(GroupDto group) {
        this.selectedGroup = group;
        System.out.println("MainController: Selected group updated to: " + (group != null ? group.getName() : "None"));

        // If the current view is a GroupDetailController, update it
        if (currentLoadedController instanceof GroupDetailController) {
            ((GroupDetailController) currentLoadedController).setGroup(group);
        }
        // If the current view is TasksController or ResourcesController, update them
        // This logic might need refinement if these controllers are loaded directly
        // For now, they will be updated when GroupDetailController loads them.
    }

    @FXML
    private void initialize() {
        System.out.println("Main screen loaded. Waiting for user info...");
        // Set initial title
        sectionTitleLabel.setText("Dashboard");
    }

    @FXML
    private void handleSidebarNavigation(javafx.event.ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String fxmlPath = "";
        String title = "";

        if (sourceButton == myGroupsButton) {
            fxmlPath = "/fxml/groups-view.fxml";
            title = "My Groups";
        } else if (sourceButton == tasksAllButton) {
            fxmlPath = "/fxml/tasks-view.fxml"; // This will show all tasks or tasks for selected group
            title = "Tasks";
        } else if (sourceButton == statisticsButton) {
            fxmlPath = "/fxml/statistics-view.fxml";
            title = "Statistics";
        } else if (sourceButton == profileButton) {
            fxmlPath = "/fxml/profile-view.fxml";
            title = "Profile";
        }

        if (!fxmlPath.isEmpty()) {
            loadView(fxmlPath, title);
        }
    }

    // New method to load Group Detail View
    public void loadGroupDetailView(GroupDto group) {
        this.selectedGroup = group; // Set the selected group
        loadView("/fxml/group-detail-view.fxml", group.getName());
    }

    private void loadView(String fxmlPath, String title) {
        System.out.println("MainController: Attempting to load view: " + fxmlPath + " with title: " + title);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Pane view = loader.load();
            System.out.println("MainController: Successfully loaded FXML: " + fxmlPath);

            // Pass ApiService and user info to the loaded controller
            Object controller = loader.getController();
            System.out.println("MainController: Loaded controller for " + fxmlPath + ": " + controller.getClass().getSimpleName());
            if (controller instanceof BaseController) {
                currentLoadedController = (BaseController) controller; // Store reference to current controller
                currentLoadedController.setApiService(apiService);
                currentLoadedController.setCurrentUser(currentUserId, currentNickname);
                currentLoadedController.setMainController(this); // Pass MainController reference
                System.out.println("MainController: Passed API service and user info to " + controller.getClass().getSimpleName());
            }

            // Specific handling for GroupDetailController
            if (controller instanceof GroupDetailController) {
                ((GroupDetailController) controller).setGroup(selectedGroup);
                System.out.println("MainController: Passed selected group to GroupDetailController.");
            }
            // For other controllers, they will load their data based on current user/apiService

            contentPane.getChildren().setAll(view);
            sectionTitleLabel.setText(title); // Update top bar title
            System.out.println("MainController: View loaded and title updated.");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load view: " + title + ". Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Error initializing controller for view: " + title + ". Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        // Clear token and user info
        apiService.setTokenAndUserId(null, null);
        // Navigate back to login screen
        try {
            Stage stage = (Stage) contentPane.getScene().getWindow(); // Use contentPane to get the stage
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/auth-view.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            stage.setScene(scene);
            stage.setTitle("Combined Study Platform - Welcome");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Could not load login screen after logout.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
