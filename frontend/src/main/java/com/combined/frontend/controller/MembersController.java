package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.dto.MemberWithRoleDto;
import com.combined.backend.dto.TaskDto;
import com.combined.backend.model.Membership; // New import
import com.combined.frontend.service.ApiService;
import com.combined.frontend.util.ErrorUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class MembersController implements BaseController {

    @FXML private ListView<MemberWithRoleDto> membersListView;

    @FXML private Label statusLabel;
    @FXML private Button addMemberButton;
    @FXML private Button refreshButton;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentGroup;
    private boolean canManageMembers = false;
    private Membership.Role currentUserRoleInGroup = null;
    private final Map<Long, Membership.Role> pendingRoleChanges = new ConcurrentHashMap<>();

    private final ObservableList<MemberWithRoleDto> membersData = FXCollections.observableArrayList();
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

    public void refreshMembers() {
        if (currentGroup != null) {
            loadGroupMembers();
        }
    }

    public void setGroup(GroupDto group) {
        this.currentGroup = group;
        this.canManageMembers = group != null && currentUserId != null && currentUserId.equals(group.getCreatedById());
        if (addMemberButton != null) {
            addMemberButton.setDisable(!canManageMembers);
        }
        if (refreshButton != null) {
            refreshButton.setDisable(currentGroup == null);
        }
        if (group != null) {
            loadGroupMembers();
        } else {
            membersData.clear();
            statusLabel.setText("No group selected.");
            currentUserRoleInGroup = null;
            canManageMembers = false;
        }
    }

    @FXML
    private void initialize() {
        membersListView.setItems(membersData);
        membersListView.setPlaceholder(new Label("No members"));
        membersListView.setCellFactory(list -> new MemberCell());
        statusLabel.setText("Select a group to view members.");
    }

    @FXML
    private void onRefreshClick() {
        if (currentGroup != null) {
            loadGroupMembers();
        } else {
            statusLabel.setText("No group selected to refresh members.");
        }
    }

    @FXML
    private void onAddMemberClick() {
        if (currentGroup == null) {
            showAlert(Alert.AlertType.WARNING, "Add Member", "Please select a group first to add a member.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-member-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            AddMemberController addMemberController = loader.getController();
            addMemberController.setApiService(apiService);
            addMemberController.setGroupId(currentGroup.getId());
            addMemberController.setGroupName(currentGroup.getName());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Add Member to " + currentGroup.getName());
            dialog.setResultConverter(buttonType -> {
                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    return addMemberController.processResult() ? buttonType : null; // null keeps dialog open on failure
                }
                return buttonType;
            });

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Member added successfully!");
                loadGroupMembers(); // Refresh list
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load add member dialog", e));
        }
    }

    private void handleRemoveMember(MemberWithRoleDto member) {
        int unfinishedCount = 0;
        try {
            List<TaskDto> tasks = apiService.getTasksByGroup(currentGroup.getId());
            unfinishedCount = (int) tasks.stream()
                    .filter(t -> member.getUserId().equals(t.getAssignedToId()))
                    .filter(t -> {
                        String s = t.getStatus() == null ? "" : t.getStatus().toUpperCase();
                        return !s.equals("DONE") && !s.equals("COMPLETED");
                    })
                    .count();
        } catch (Exception e) {
            // If we cannot load tasks, proceed but note the issue
            System.err.println("MembersController: Could not load tasks before removal check: " + e.getMessage());
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Remove");
        confirmAlert.setHeaderText("Remove Member: " + member.getNickname());
        StringBuilder content = new StringBuilder("Are you sure you want to remove ")
                .append(member.getNickname())
                .append(" from ")
                .append(currentGroup.getName())
                .append("?");
        if (unfinishedCount > 0) {
            content.append("\n\nWarning: this member has ")
                    .append(unfinishedCount)
                    .append(" unfinished task(s). They will be unassigned.");
        }
        confirmAlert.setContentText(content.toString());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    apiService.removeMember(currentGroup.getId(), member.getUserId());
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Member removed successfully!");
                        loadGroupMembers(); // Refresh list
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Failed to remove member", e));
                    });
                }
            }).start();
        }
    }

    private void handleChangeRole(MemberWithRoleDto member, Membership.Role newRole) {
        new Thread(() -> {
            try {
                apiService.updateMemberRole(currentGroup.getId(), member.getUserId(), newRole);
                Platform.runLater(() -> loadGroupMembers()); // Refresh list without popup
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Failed to update member role", e));
                });
            } finally {
                pendingRoleChanges.remove(member.getUserId());
            }
        }).start();
    }

    private void loadGroupMembers() {
        if (apiService == null || currentGroup == null) {
            statusLabel.setStyle("-fx-text-fill: orange;");
            statusLabel.setText("API Service or Group not set.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading members for group " + currentGroup.getName() + "...");

        new Thread(() -> {
            try {
                List<MemberWithRoleDto> memberList = apiService.getGroupMembers(currentGroup.getId());
                currentUserRoleInGroup = memberList.stream()
                        .filter(m -> m.getUserId().equals(currentUserId))
                        .findFirst()
                        .map(MemberWithRoleDto::getRole)
                        .orElse(null);
                canManageMembers = currentGroup.getCreatedById().equals(currentUserId) ||
                        currentUserRoleInGroup == Membership.Role.ADMIN;
                Platform.runLater(() -> {
                    // re-bind items to ensure the table is pointing at the latest list
                    ObservableList<MemberWithRoleDto> newData = FXCollections.observableArrayList(memberList);
                    membersData.setAll(newData);
                    addMemberButton.setDisable(!canManageMembers);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + memberList.size() + " members.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText(ErrorUtils.formatError("Error loading members", e));
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

    /**
     * Custom list cell to render member info with role selector and remove button.
     */
    private class MemberCell extends ListCell<MemberWithRoleDto> {
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final ComboBox<Membership.Role> roleCombo = new ComboBox<>(
                FXCollections.observableArrayList(Membership.Role.ADMIN, Membership.Role.MEMBER));
        private final Button removeButton = new Button("Remove");
        private final HBox root = new HBox(10);
        private final Region spacer = new Region();
        private MemberWithRoleDto currentUser;
        private boolean applyingRole = false;

        MemberCell() {
            roleCombo.setPrefWidth(140);
            removeButton.setOnAction(e -> {
                if (currentUser != null) {
                    handleRemoveMember(currentUser);
                }
            });
            root.getChildren().addAll(nameLabel, emailLabel, spacer, roleCombo, removeButton);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (currentUser == null || newVal == null) return;
                if (applyingRole) return; // ignore programmatic set during rendering
                if (currentUser.getRole() == newVal) return; // no-op if unchanged
                boolean isCreator = currentGroup != null && currentUser.getUserId().equals(currentGroup.getCreatedById());
                boolean isSelf = currentUser.getUserId().equals(currentUserId);
                if (isCreator || isSelf || !canManageMembers) return;
                Membership.Role pending = pendingRoleChanges.get(currentUser.getUserId());
                if (pending != null && pending == newVal) return; // debounce identical in-flight
                pendingRoleChanges.put(currentUser.getUserId(), newVal);
                handleChangeRole(currentUser, newVal);
            });
        }

        @Override
        protected void updateItem(MemberWithRoleDto item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                currentUser = null;
                setGraphic(null);
            } else {
                this.currentUser = item;
                String displayName = item.getNickname();
                if (item.getFirstName() != null && !item.getFirstName().isBlank()) {
                    displayName = item.getFirstName();
                    if (item.getLastName() != null && !item.getLastName().isBlank()) {
                        displayName += " " + item.getLastName();
                    }
                }
                nameLabel.setText("#" + item.getUserId() + "  " + displayName);
                emailLabel.setText("<" + item.getEmail() + ">");

                boolean isCreator = currentGroup != null && item.getUserId().equals(currentGroup.getCreatedById());
                boolean isSelf = item.getUserId().equals(currentUserId);
                boolean actorCanManageThisRow = canManageMembers && !isCreator && !isSelf;
                Membership.Role role = item.getRole();
                applyingRole = true;
                if (role == Membership.Role.CREATOR) {
                    roleCombo.getSelectionModel().clearSelection();
                } else {
                    roleCombo.getSelectionModel().select(role != null ? role : Membership.Role.MEMBER);
                }
                applyingRole = false;
                if (isCreator) {
                    roleCombo.setPromptText("CREATOR");
                }
                roleCombo.setDisable(!actorCanManageThisRow);
                removeButton.setDisable(!actorCanManageThisRow);

                setGraphic(root);
            }
        }
    }
}
