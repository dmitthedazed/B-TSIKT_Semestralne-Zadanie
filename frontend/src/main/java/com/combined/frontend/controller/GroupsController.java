package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GroupsController implements BaseController {

    @FXML private TabPane groupsTabPane; // New TabPane for My Groups and All Groups
    @FXML private TableView<GroupDto> myGroupsTable;
    @FXML private TableColumn<GroupDto, String> myGroupNameColumn;
    @FXML private TableColumn<GroupDto, String> myGroupDescriptionColumn;
    @FXML private TableColumn<GroupDto, String> myGroupCreatedByColumn;
    @FXML private TableColumn<GroupDto, Long> myGroupMembersColumn;
    @FXML private TableColumn<GroupDto, Long> myGroupOpenTasksColumn;
    @FXML private TableColumn<GroupDto, Void> myGroupActionColumn; // For Open button

    @FXML private TableView<GroupDto> allGroupsTable;
    @FXML private TableColumn<GroupDto, String> allGroupNameColumn;
    @FXML private TableColumn<GroupDto, String> allGroupDescriptionColumn;
    @FXML private TableColumn<GroupDto, String> allGroupCreatedByColumn;
    @FXML private TableColumn<GroupDto, Long> allGroupMembersColumn;
    @FXML private TableColumn<GroupDto, Void> allGroupActionColumn; // For Join button

    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;

    private final ObservableList<GroupDto> myGroupsData = FXCollections.observableArrayList();
    private final ObservableList<GroupDto> allGroupsData = FXCollections.observableArrayList();

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

    @FXML
    private void initialize() {
        // My Groups Table Setup
        myGroupNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        myGroupDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        myGroupCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdByName"));
        myGroupMembersColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
        myGroupOpenTasksColumn.setCellValueFactory(new PropertyValueFactory<>("openTaskCount"));
        myGroupsTable.setItems(myGroupsData);
        addOpenButtonToMyGroupsTable();

        // All Groups Table Setup
        allGroupNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        allGroupDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        allGroupCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdByName"));
        allGroupMembersColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
        allGroupsTable.setItems(allGroupsData);
        addJoinButtonToAllGroupsTable();

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

    private void addOpenButtonToMyGroupsTable() {
        myGroupActionColumn.setCellFactory(param -> new TableCell<GroupDto, Void>() {
            private final Button openButton = new Button("Open");
            {
                openButton.setOnAction(event -> {
                    GroupDto group = getTableView().getItems().get(getIndex());
                    if (mainController != null) {
                        mainController.updateSelectedGroup(group);
                        mainController.loadGroupDetailView(group);
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Group Detail", "Opening details for group: " + group.getName());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(openButton);
                }
            }
        });
    }

    private void addJoinButtonToAllGroupsTable() {
        allGroupActionColumn.setCellFactory(param -> new TableCell<GroupDto, Void>() {
            private final Button joinButton = new Button("Join");
            {
                joinButton.setOnAction(event -> {
                    GroupDto group = getTableView().getItems().get(getIndex());
                    handleJoinGroup(group);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    GroupDto group = getTableView().getItems().get(getIndex());
                    if (group.getIsMember()) { // If already a member, show "Joined" or disable button
                        setGraphic(new Label("Joined"));
                    } else {
                        setGraphic(joinButton);
                    }
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
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to create group. Check input.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load create group dialog.");
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
                    showAlert(Alert.AlertType.ERROR, "Join Group Failed", e.getMessage());
                });
            }
        }).start();
    }

    private void loadGroups() {
        loadMyGroups();
        loadAllGroups();
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
                    myGroupsData.setAll(groupList);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + groupList.size() + " of my groups.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading my groups: " + e.getMessage());
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
                List<GroupDto> allGroups = apiService.getAllGroupsWithMembershipFlag(currentUserId);
                List<GroupDto> nonMemberGroups = allGroups.stream()
                        .filter(g -> !g.getIsMember())
                        .toList();

                Platform.runLater(() -> {
                    allGroupsData.setAll(nonMemberGroups);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + nonMemberGroups.size() + " available groups.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading all groups: " + e.getMessage());
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
}
