package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.ResourceDto;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.dto.MemberWithRoleDto;
import com.combined.frontend.util.ErrorUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Paths;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ResourcesController implements BaseController {

    @FXML private ListView<ResourceDto> resourcesListView;

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

    public void refreshResources() {
        if (currentlySelectedGroup != null) {
            determineRoleAndLoad(currentlySelectedGroup);
        }
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
        resourcesListView.setItems(resourcesData);
        resourcesListView.setCellFactory(list -> new ResourceCell());
        statusLabel.setText("Select a group to view resources.");
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
            createResourceController.setMainController(mainController);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Create New Resource for " + currentlySelectedGroup.getName());

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, evt -> {
                boolean ok = createResourceController.processResult();
                if (!ok) {
                    evt.consume();
                }
            });

            dialog.showAndWait();
            if (createResourceController.wasSaved()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Resource created successfully!");
                loadResourcesForGroup(currentlySelectedGroup.getId()); // Refresh list
            } else {
                String reason = createResourceController.getLastErrorMessage();
                if (reason != null && !reason.isBlank()) {
                    showAlert(Alert.AlertType.ERROR, "Error", reason);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load create resource dialog", e));
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
            editResourceController.setMainController(mainController);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Resource: " + resource.getTitle());

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, evt -> {
                boolean ok = editResourceController.processEditResult(resource.getId());
                if (!ok) {
                    evt.consume();
                }
            });

            dialog.showAndWait();
            if (editResourceController.wasSaved()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Resource updated successfully!");
                loadResourcesForGroup(currentlySelectedGroup.getId()); // Refresh list
            } else {
                String reason = editResourceController.getLastErrorMessage();
                if (reason != null && !reason.isBlank()) {
                    showAlert(Alert.AlertType.ERROR, "Error", reason);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load edit resource dialog", e));
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
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Failed to delete resource", e)));
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
            showAlert(Alert.AlertType.ERROR, "Open/Download", ErrorUtils.formatError("Cannot open resource", ex));
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
                    statusLabel.setText(ErrorUtils.formatError("Error loading resources", e));
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
                    statusLabel.setText(ErrorUtils.formatError("Error determining role", e));
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

    private class ResourceCell extends ListCell<ResourceDto> {
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Label desc = new Label();
        private final Button openButton = new Button();
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final VBox content = new VBox(6);
        private final HBox actions = new HBox(6);

        ResourceCell() {
            actions.getChildren().addAll(openButton, editButton, deleteButton);
            content.getChildren().addAll(title, meta, desc, actions);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border; -fx-border-radius: 6; -fx-background-radius: 6;");
            content.setFillWidth(true);
            if (resourcesListView != null) {
                content.prefWidthProperty().bind(resourcesListView.widthProperty().subtract(30));
            }
            openButton.setOnAction(e -> {
                ResourceDto res = getItem();
                if (res != null) handleOpenOrDownload(res);
            });
            editButton.setOnAction(e -> {
                ResourceDto res = getItem();
                if (res != null) handleEditResource(res);
            });
            deleteButton.setOnAction(e -> {
                ResourceDto res = getItem();
                if (res != null) handleDeleteResource(res);
            });
        }

        @Override
        protected void updateItem(ResourceDto res, boolean empty) {
            super.updateItem(res, empty);
            if (empty || res == null) {
                setGraphic(null);
                setText(null);
            } else {
                title.setStyle("-fx-text-fill: #1d1d1f; -fx-font-weight: bold;");
                meta.setStyle("-fx-text-fill: #4a4a4a;");
                desc.setStyle("-fx-text-fill: #2f2f2f;");
                String type = res.getType() == null ? "" : res.getType().toUpperCase();
                title.setText(res.getTitle() + " (" + type + ")");
                meta.setText((res.getPathOrUrl() == null ? "" : res.getPathOrUrl()) +
                        (res.getUploadedByName() == null ? "" : " | " + res.getUploadedByName()));
                desc.setText(res.getDescription() == null ? "" : res.getDescription());
                boolean isFile = type.equals("FILE");
                openButton.setText(isFile ? "Download" : "Open Link");
                boolean canModify = canModifyResource(res);
                editButton.setDisable(!canModify);
                deleteButton.setDisable(!canModify);
                if (getListView() != null) {
                    content.prefWidthProperty().bind(getListView().widthProperty().subtract(30));
                }
                setText(null);
                setGraphic(content);
            }
        }
    }
}
