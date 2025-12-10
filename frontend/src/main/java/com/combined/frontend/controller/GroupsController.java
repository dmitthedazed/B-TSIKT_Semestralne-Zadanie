package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.service.NotificationAware;
import com.combined.frontend.service.NotificationService;
import com.combined.frontend.util.ErrorUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.animation.ScaleTransition;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GroupsController implements BaseController, NotificationAware {

    @FXML private TabPane groupsTabPane; // New TabPane for My Groups and All Groups
    @FXML private FlowPane myGroupsFlow;
    @FXML private FlowPane allGroupsFlow;

    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private NotificationService notificationService;
    private boolean subscribedOnce = false;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        loadGroups(); // Load groups after user is set
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");

        // Add listener to TabPane to load data when tabs are selected
        groupsTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null) {
                if ("myGroupsTab".equals(newTab.getId())) {
                    loadMyGroups();
                } else if ("allGroupsTab".equals(newTab.getId())) {
                    loadAllGroups();
                }
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        loadGroups();
    }

    @FXML
    private void onCreateGroupClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-group-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            CreateGroupController createGroupController = loader.getController();
            createGroupController.setApiService(apiService);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Create New Group");

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (createGroupController.processResult()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Group created successfully!");
                    loadGroups(); // Refresh all group lists
                } else {
                    String reason = createGroupController.getLastErrorMessage();
                    String message = (reason == null || reason.isBlank())
                            ? "Failed to create group. No details were provided."
                            : reason;
                    showAlert(Alert.AlertType.ERROR, "Error", message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load create group dialog", e));
        }
    }

    private void handleJoinGroup(GroupDto group) {
        if (apiService == null || currentUserId == null) {
            showAlert(Alert.AlertType.ERROR, "Join Group", "User not logged in or ApiService not set.");
            return;
        }
        new Thread(() -> {
            try {
                apiService.joinGroup(group.getId());
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Join Group", "You joined the group: " + group.getName());
                    loadGroups(); // Refresh both lists
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Join Group Failed", ErrorUtils.formatError("Failed to join group", e));
                });
            }
        }).start();
    }

    private void loadGroups() {
        loadMyGroups();
        loadAllGroups();
    }

    private boolean handleUnauthorized(Exception e, String context) {
        String message = ErrorUtils.extractReason(e).toLowerCase();
        if (!message.isBlank() && message.contains("unauthorized")) {
            if (mainController != null) {
                mainController.handleUnauthorized(context);
            }
            return true;
        }
        return false;
    }

    private void loadMyGroups() {
        if (apiService == null || currentUserId == null) {
            statusLabel.setStyle("-fx-text-fill: orange;");
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading my groups...");

        new Thread(() -> {
            try {
                List<GroupDto> groupList = apiService.getMyGroups();

                Platform.runLater(() -> {
                    renderGroups(myGroupsFlow, groupList, false);
                    if (notificationService != null) {
                        if (subscribedOnce) {
                            // skip re-subscribing on subsequent refreshes
                        } else {
                            subscribedOnce = true;
                            groupList.stream()
                                    .map(GroupDto::getId)
                                    .filter(id -> id != null)
                                    .forEach(notificationService::subscribeToGroup); // ensure subscriptions exist for global handler
                        }
                    }
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + groupList.size() + " of my groups.");
                });
            } catch (Exception e) {
                if (handleUnauthorized(e, "groups")) return;
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText(ErrorUtils.formatError("Error loading my groups", e));
                });
            }
        }).start();
    }

    private void loadAllGroups() {
        if (apiService == null || currentUserId == null) {
            statusLabel.setStyle("-fx-text-fill: orange;");
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading all available groups...");

        new Thread(() -> {
            try {
                List<GroupDto> allGroups = apiService.getAllGroupsWithMembershipFlag();
                List<GroupDto> nonMemberGroups = allGroups.stream()
                        .filter(g -> !g.getIsMember())
                        .toList();

                Platform.runLater(() -> {
                    renderGroups(allGroupsFlow, nonMemberGroups, true);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    if (nonMemberGroups.isEmpty()) {
                        statusLabel.setText("You have subscribed to all courses! Good luck with that.");
                        allGroupsFlow.getChildren().clear();
                        Label empty = new Label("You have subscribed to all courses! Good luck with that.");
                        empty.setStyle("-fx-text-fill: #444; -fx-font-size: 14px;");
                        empty.setWrapText(true);
                        allGroupsFlow.getChildren().add(empty);
                    } else {
                        statusLabel.setText("Loaded " + nonMemberGroups.size() + " available groups.");
                    }
                });
            } catch (Exception e) {
                if (handleUnauthorized(e, "groups")) return;
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText(ErrorUtils.formatError("Error loading all groups", e));
                });
            }
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void renderGroups(FlowPane container, List<GroupDto> groups, boolean showJoin) {
        if (container == null) return;
        container.getChildren().clear();
        for (GroupDto g : groups) {
            container.getChildren().add(buildGroupCard(g, showJoin));
        }
    }

    private VBox buildGroupCard(GroupDto group, boolean showJoin) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setMinWidth(220);
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border; -fx-border-radius: 6; -fx-background-radius: 6;");
        DropShadow hoverShadow = new DropShadow(10, Color.rgb(44, 123, 229, 0.35));
        ScaleTransition scaleUp = new ScaleTransition(javafx.util.Duration.millis(150), card);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        ScaleTransition scaleDown = new ScaleTransition(javafx.util.Duration.millis(150), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        card.setOnMouseClicked(e -> {
            if (showJoin) {
                handleJoinGroup(group);
            } else if (mainController != null) {
                mainController.updateSelectedGroup(group);
                mainController.loadGroupDetailView(group);
            }
        });
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: derive(-fx-control-inner-background,5%); -fx-border-color: -fx-box-border; -fx-border-radius: 6; -fx-background-radius: 6;");
            scaleDown.stop();
            scaleUp.playFromStart();
            card.setEffect(hoverShadow);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border; -fx-border-radius: 6; -fx-background-radius: 6;");
            scaleUp.stop();
            scaleDown.playFromStart();
            card.setEffect(null);
        });

        Label name = new Label(group.getName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label desc = new Label(group.getDescription() == null ? "" : group.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #555;");

        Label owner = new Label("Owner: " + ownerName(group));
        owner.setStyle("-fx-text-fill: #333;");

        HBox badges = new HBox(6);
        badges.setPadding(new Insets(4, 0, 0, 0));
        badges.getChildren().add(buildBadge("👥", String.valueOf(group.getMemberCount())));
        badges.getChildren().add(buildBadge("🗒", String.valueOf(group.getOpenTaskCount())));

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button primary = new Button((showJoin ? "Join" : "Open") + " →");
        primary.setOnAction(e -> {
            e.consume();
            if (showJoin) {
                handleJoinGroup(group);
            } else if (mainController != null) {
                mainController.updateSelectedGroup(group);
                mainController.loadGroupDetailView(group);
            }
        });

        actions.getChildren().add(primary);
        card.getChildren().addAll(name, desc, owner, badges, actions);
        return card;
    }

    private Label buildBadge(String icon, String text) {
        Label badge = new Label(icon + " " + text);
        badge.setStyle("-fx-background-color: derive(-fx-control-inner-background, -5%); -fx-border-color: -fx-box-border; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 4 8; -fx-font-size: 11px;");
        return badge;
    }

    private String ownerName(GroupDto group) {
        return nullSafe(group.getCreatedByName());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
