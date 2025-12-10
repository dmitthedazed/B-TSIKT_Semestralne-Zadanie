package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.ResourceDto;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.dto.MemberWithRoleDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox; // New import
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ResourcesController implements BaseController {

    @FXML private TableView<ResourceDto> resourcesTable;
    @FXML private TableColumn<ResourceDto, Long> idColumn;
    @FXML private TableColumn<ResourceDto, String> titleColumn;
    @FXML private TableColumn<ResourceDto, String> urlColumn;
    @FXML private TableColumn<ResourceDto, String> typeColumn; // Will show extension for files, LINK for links
    @FXML private TableColumn<ResourceDto, String> descriptionColumn;
    @FXML private TableColumn<ResourceDto, String> uploadedByColumn; // New column
    @FXML private TableColumn<ResourceDto, Void> actionColumn; // For Edit/Delete buttons

    @FXML private Label statusLabel;
    @FXML private Button createResourceButton;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentlySelectedGroup;
    private String currentRole = "MEMBER"; // CREATOR/ADMIN/MEMBER

    private final ObservableList<ResourceDto> resourcesData = FXCollections.observableArrayList();

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

    public void setCurrentlySelectedGroup(GroupDto group) {
        this.currentlySelectedGroup = group;
        if (group != null) {
            determineRoleAndLoad(group);
        } else {
            resourcesData.clear();
            statusLabel.setText("No group selected.");
        }
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("pathOrUrl"));
        typeColumn.setCellValueFactory(param -> {
            ResourceDto r = param.getValue();
            if (r == null) return null;
            String ext = (r.getFileExtension() != null && !r.getFileExtension().isBlank()) ? r.getFileExtension() : null;
            String type = (r.getType() != null && r.getType().equalsIgnoreCase("FILE"))
                    ? (ext != null ? ext : "FILE")
                    : "LINK";
            return new javafx.beans.property.SimpleStringProperty(type);
        });
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        uploadedByColumn.setCellValueFactory(new PropertyValueFactory<>("uploadedByName"));

        resourcesTable.setItems(resourcesData);
        statusLabel.setText("Select a group to view resources.");

        addActionButtonToTable();
    }

    private void addActionButtonToTable() {
        actionColumn.setCellFactory(param -> new TableCell<ResourceDto, Void>() {
            private final Button openButton = new Button();
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(openButton, editButton, deleteButton);

            {
                pane.setSpacing(5);
                openButton.setOnAction(event -> {
                    ResourceDto resource = getTableView().getItems().get(getIndex());
                    handleOpenOrDownload(resource);
                });
                editButton.setOnAction(event -> {
                    ResourceDto resource = getTableView().getItems().get(getIndex());
                    handleEditResource(resource);
                });
                deleteButton.setOnAction(event -> {
                    ResourceDto resource = getTableView().getItems().get(getIndex());
                    handleDeleteResource(resource);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ResourceDto resource = getTableView().getItems().get(getIndex());
                    boolean isFile = resource != null && resource.getType() != null && resource.getType().equalsIgnoreCase("FILE");
                    openButton.setText(isFile ? "Download" : "Open Link");
                    openButton.setDisable(resource == null || resource.getPathOrUrl() == null || resource.getPathOrUrl().isBlank());
                    boolean canModify = canModifyResource(resource);
                    editButton.setDisable(!canModify);
                    deleteButton.setDisable(!canModify);
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    private void onRefreshClick() {
        if (currentlySelectedGroup != null) {
            loadResourcesForGroup(currentlySelectedGroup.getId());
        } else {
            statusLabel.setText("No group selected to refresh resources.");
        }
    }

    @FXML
    private void onCreateResourceClick() {
        if (currentlySelectedGroup == null) {
            showAlert(Alert.AlertType.WARNING, "Create Resource", "Please select a group first to create a resource.");
            return;
        }
        if (!canCreate()) {
            showAlert(Alert.AlertType.WARNING, "Permission", "You cannot create resources in this group.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-resource-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            CreateResourceController createResourceController = loader.getController();
            createResourceController.setApiService(apiService);
            createResourceController.setGroupId(currentlySelectedGroup.getId());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Create New Resource for " + currentlySelectedGroup.getName());

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (createResourceController.processResult()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Resource created successfully!");
                    loadResourcesForGroup(currentlySelectedGroup.getId()); // Refresh list
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to create resource. Check input.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load create resource dialog.");
        }
    }

    private void handleEditResource(ResourceDto resource) {
        if (!canModifyResource(resource)) {
            showAlert(Alert.AlertType.WARNING, "Permission", "You cannot edit this resource.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-resource-dialog.fxml")); // Re-use create dialog for edit
            DialogPane dialogPane = loader.load();
            CreateResourceController editResourceController = loader.getController();
            editResourceController.setApiService(apiService);
            editResourceController.setGroupId(currentlySelectedGroup.getId());
            editResourceController.setResourceToEdit(resource); // New method to pre-fill form

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Resource: " + resource.getTitle());

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (editResourceController.processEditResult(resource.getId())) { // New method for processing edit
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Resource updated successfully!");
                    loadResourcesForGroup(currentlySelectedGroup.getId()); // Refresh list
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update resource. Check input.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load edit resource dialog.");
        }
    }

    private void handleDeleteResource(ResourceDto resource) {
        if (!canModifyResource(resource)) {
            showAlert(Alert.AlertType.WARNING, "Permission", "You cannot delete this resource.");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Resource: " + resource.getTitle());
        confirmAlert.setContentText("Are you sure you want to delete this resource?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    apiService.deleteResource(resource.getId());
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Resource deleted successfully!");
                        loadResourcesForGroup(currentlySelectedGroup.getId()); // Refresh list
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete resource: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    private void handleOpenOrDownload(ResourceDto resource) {
        if (resource == null || resource.getPathOrUrl() == null || resource.getPathOrUrl().isBlank()) return;
        String pathOrUrl = resource.getPathOrUrl();
        try {
            if (resource.getType() != null && resource.getType().equalsIgnoreCase("FILE")) {
                // Download via API (adds auth header) then open locally
                String suggested = resource.getFileExtension() != null && !resource.getFileExtension().isBlank()
                        ? "file." + resource.getFileExtension()
                        : Paths.get(pathOrUrl).getFileName().toString();
                var temp = apiService.downloadResourceToTemp(resource.getId(), suggested);
                Desktop.getDesktop().open(temp.toFile());
            } else {
                Desktop.getDesktop().browse(new URI(pathOrUrl));
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Open/Download", "Cannot open resource: " + ex.getMessage());
        }
    }

    public void loadResourcesForGroup(Long groupId) {
        if (apiService == null || currentUserId == null) {
            statusLabel.setStyle("-fx-text-fill: orange;");
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }
        if (groupId == null) {
            resourcesData.clear();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("No group selected to load resources.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading resources for group " + groupId + "...");

        new Thread(() -> {
            try {
                List<ResourceDto> resourceList = apiService.getResourcesByGroup(groupId);

                Platform.runLater(() -> {
                    resourcesData.setAll(resourceList);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + resourceList.size() + " resources for group " + groupId + ".");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error loading resources: " + e.getMessage());
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

    private void determineRoleAndLoad(GroupDto group) {
        this.currentlySelectedGroup = group;
        this.currentRole = "MEMBER";
        if (group.getCreatedById() != null && group.getCreatedById().equals(currentUserId)) {
            this.currentRole = "CREATOR";
            updateCreateButton();
            loadResourcesForGroup(group.getId());
            return;
        }
        new Thread(() -> {
            try {
                List<MemberWithRoleDto> members = apiService.getGroupMembers(group.getId());
                String role = members.stream()
                        .filter(m -> m.getUserId().equals(currentUserId))
                        .map(MemberWithRoleDto::getRole)
                        .map(Enum::name)
                        .findFirst()
                        .orElse("MEMBER");
                this.currentRole = role;
                Platform.runLater(() -> {
                    updateCreateButton();
                    loadResourcesForGroup(group.getId());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Error determining role: " + e.getMessage());
                });
            }
        }).start();
    }

    private boolean canCreate() {
        return "CREATOR".equals(currentRole) || "ADMIN".equalsIgnoreCase(currentRole);
    }

    private boolean canModifyResource(ResourceDto resource) {
        if (resource == null) return false;
        if (canCreate()) return true; // creator/admin
        return resource.getUploadedById() != null && resource.getUploadedById().equals(currentUserId);
    }

    private void updateCreateButton() {
        if (createResourceButton != null) {
            createResourceButton.setDisable(!canCreate());
        }
    }
}
