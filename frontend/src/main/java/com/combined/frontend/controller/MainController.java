package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.MainApp;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.service.NotificationAware;
import com.combined.frontend.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

public class MainController {

    @FXML private Label sectionTitleLabel;
    @FXML private Label usernameLabel;
    @FXML private StackPane contentPane; // Central content area

    // Sidebar Buttons
    @FXML private Button myGroupsButton;
    @FXML private Button tasksAllButton;
    @FXML private Button statisticsButton;
    @FXML private Button notificationsButton;
    @FXML private Button profileButton;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private String currentFirstName;
    private GroupDto selectedGroup; // To hold the currently selected group
    private String activeViewPath;
    private NotificationService notificationService;
    private VBox toastContainer;

    // References to child controllers (only for the currently loaded view)
    private BaseController currentLoadedController;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        if (notificationService != null) {
            notificationService.registerGlobalHandler(this::handleNotification);
        }
    }

    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        this.currentFirstName = nickname;
        updateGreetingLabel();
        fetchAndSetFirstName();
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
        setupToastLayer();
    }

    private void updateGreetingLabel() {
        if (usernameLabel == null) return;
        String name = (currentFirstName != null && !currentFirstName.isBlank())
                ? currentFirstName
                : (currentNickname != null && !currentNickname.isBlank() ? currentNickname : "there");
        usernameLabel.setText(buildGreeting() + ", " + name + "!");
    }

    private String buildGreeting() {
        if (ThreadLocalRandom.current().nextDouble() < 0.03) {
            return "How is it going";
        }
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) return "Good morning";
        if (hour >= 12 && hour < 17) return "Good afternoon";
        if (hour >= 17 && hour < 22) return "Good evening";
        return "Good night";
    }

    private void fetchAndSetFirstName() {
        if (apiService == null || currentUserId == null) return;
        new Thread(() -> {
            try {
                var user = apiService.getCurrentUser();
                if (user != null && user.getFirstName() != null && !user.getFirstName().isBlank()) {
                    currentFirstName = user.getFirstName();
                    Platform.runLater(this::updateGreetingLabel);
                }
            } catch (Exception ignored) {
                // keep nickname fallback
            }
        }).start();
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
            fxmlPath = "/fxml/tasks-global-view.fxml"; // Global tasks across all groups
            title = "Tasks";
        } else if (sourceButton == statisticsButton) {
            fxmlPath = "/fxml/statistics-global-view.fxml";
            title = "Statistics";
        } else if (sourceButton == notificationsButton) {
            fxmlPath = "/fxml/notification-center-view.fxml";
            title = "Notifications";
        } else if (sourceButton == profileButton) {
            fxmlPath = "/fxml/profile-view.fxml";
            title = "Profile";
        }

        if (!fxmlPath.isEmpty()) {
            loadView(fxmlPath, title, false);
        }
    }

    // New method to load Group Detail View
    public void loadGroupDetailView(GroupDto group) {
        this.selectedGroup = group; // Set the selected group
        loadView("/fxml/group-detail-view.fxml", group.getName(), true); // force reload to reflect selected group
    }

    private void loadView(String fxmlPath, String title) {
        loadView(fxmlPath, title, false);
    }

    private void loadView(String fxmlPath, String title, boolean forceReload) {
        if (!forceReload && activeViewPath != null && activeViewPath.equals(fxmlPath)) {
            sectionTitleLabel.setText(title);
            System.out.println("MainController: Skipping reload of same view " + fxmlPath);
            return;
        }
        System.out.println("MainController: Attempting to load view: " + fxmlPath + " with title: " + title);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            System.out.println("MainController: Successfully loaded FXML: " + fxmlPath);

            // Pass ApiService and user info to the loaded controller
            Object controller = loader.getController();
            System.out.println("MainController: Loaded controller for " + fxmlPath + ": " + controller.getClass().getSimpleName());
            if (controller instanceof NotificationAware && notificationService != null) {
                ((NotificationAware) controller).setNotificationService(notificationService);
            }
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
            attachToastLayer();
            sectionTitleLabel.setText(title); // Update top bar title
            activeViewPath = fxmlPath;
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
        if (notificationService != null) {
            notificationService.disconnect();
        }
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

    public void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleUnauthorized(String context) {
        Platform.runLater(() -> {
            showAlert(Alert.AlertType.ERROR, "Session issue", "Please re-login to continue (" + context + ").");
            handleLogout();
        });
    }

    private void setupToastLayer() {
        toastContainer = new VBox(8);
        toastContainer.setMouseTransparent(true);
        toastContainer.setPadding(new Insets(10));
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setPickOnBounds(false);
        contentPane.getChildren().add(toastContainer);
        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
    }

    private void attachToastLayer() {
        if (toastContainer != null && !contentPane.getChildren().contains(toastContainer)) {
            contentPane.getChildren().add(toastContainer);
            StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
        }
    }

    private void handleNotification(NotificationService.NotificationMessage msg) {
        if (msg == null) return;
        String text = msg.getMessage() != null && !msg.getMessage().isBlank()
                ? msg.getMessage()
                : (msg.getType() != null ? msg.getType() : "Update");
        showToast(text);
    }

    private void showToast(String text) {
        Platform.runLater(() -> {
            attachToastLayer(); // ensure overlay survives view swaps
            Label label = new Label(text);
            label.setStyle("-fx-text-fill: white; -fx-padding: 10 14; -fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 6;");
            label.setWrapText(true);
            toastContainer.getChildren().add(label);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // auto-remove after 4 seconds
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(4));
            fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(label));
            fadeOut.play();
        });
    }
}
