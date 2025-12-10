package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.TaskDto;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.util.ErrorUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TasksController implements BaseController {

    @FXML private ListView<TaskDto> tasksListView;

    @FXML private ComboBox<String> statusFilterComboBox; // New filter
    @FXML private TextField assignedToFilterField; // New filter

    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private GroupDto currentlySelectedGroup;

    private final ObservableList<TaskDto> tasksData = FXCollections.observableArrayList();
    private List<TaskDto> allTasksInGroup; // To store all tasks for filtering

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

    public void refreshFromServer() {
        if (currentlySelectedGroup != null) {
            loadTasksForGroup(currentlySelectedGroup.getId());
        }
    }

    public void setCurrentlySelectedGroup(GroupDto group) {
        this.currentlySelectedGroup = group;
        if (group != null) {
            loadTasksForGroup(group.getId());
        } else {
            tasksData.clear();
            statusLabel.setText("No group selected.");
        }
    }

    @FXML
    private void initialize() {
        tasksListView.setItems(tasksData);
        tasksListView.setCellFactory(list -> new TaskCell());
        statusLabel.setText("Select a group to view tasks.");

        // Setup status filter
        statusFilterComboBox.setItems(FXCollections.observableArrayList("All", "OPEN", "IN_PROGRESS", "DONE", "CLOSED"));
        statusFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        assignedToFilterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    private void onRefreshClick() {
        if (currentlySelectedGroup != null) {
            loadTasksForGroup(currentlySelectedGroup.getId());
        } else {
            statusLabel.setText("No group selected to refresh tasks.");
        }
    }

    @FXML
    private void onCreateTaskClick() {
        if (currentlySelectedGroup == null) {
            showAlert(Alert.AlertType.WARNING, "Create Task", "Please select a group first to create a task.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-task-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            CreateTaskController createTaskController = loader.getController();
            createTaskController.setApiService(apiService);
            createTaskController.setGroupId(currentlySelectedGroup.getId());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Create New Task for " + currentlySelectedGroup.getName());

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (createTaskController.processResult()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Task created successfully!");
                    loadTasksForGroup(currentlySelectedGroup.getId()); // Refresh list
                } else {
                    String reason = createTaskController.getLastErrorMessage();
                    String message = (reason == null || reason.isBlank())
                            ? "Failed to create task."
                            : reason;
                    showAlert(Alert.AlertType.ERROR, "Error", message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load create task dialog", e));
        }
    }

    private void handleEditTask(TaskDto task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-task-dialog.fxml")); // Re-use create dialog for edit
            DialogPane dialogPane = loader.load();
            CreateTaskController editTaskController = loader.getController();
            editTaskController.setApiService(apiService);
            editTaskController.setGroupId(currentlySelectedGroup.getId());
            editTaskController.setTaskToEdit(task); // New method to pre-fill form

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Task: " + task.getTitle());

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (editTaskController.processEditResult(task.getId())) { // New method for processing edit
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Task updated successfully!");
                    loadTasksForGroup(currentlySelectedGroup.getId()); // Refresh list
                } else {
                    String reason = editTaskController.getLastErrorMessage();
                    String message = (reason == null || reason.isBlank())
                            ? "Failed to update task."
                            : reason;
                    showAlert(Alert.AlertType.ERROR, "Error", message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Could not load edit task dialog", e));
        }
    }

    private void handleDeleteTask(TaskDto task) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Task: " + task.getTitle());
        confirmAlert.setContentText("Are you sure you want to delete this task?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    apiService.deleteTask(task.getId());
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Task deleted successfully!");
                        loadTasksForGroup(currentlySelectedGroup.getId()); // Refresh list
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", ErrorUtils.formatError("Failed to delete task", e)));
                }
            }).start();
        }
    }

    public void loadTasksForGroup(Long groupId) {
        if (apiService == null || currentUserId == null) {
            statusLabel.setStyle("-fx-text-fill: orange;");
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }
        if (groupId == null) {
            tasksData.clear();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("No group selected to load tasks.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Loading tasks for group " + groupId + "...");

        new Thread(() -> {
            try {
                allTasksInGroup = apiService.getTasksByGroup(groupId); // Store all tasks
                Platform.runLater(() -> {
                    applyFilters(); // Apply filters to initially loaded tasks
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + allTasksInGroup.size() + " tasks for group " + groupId + ".");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText(ErrorUtils.formatError("Error loading tasks", e));
                });
            }
        }).start();
    }

    private void applyFilters() {
        if (allTasksInGroup == null) return;

        String statusFilter = statusFilterComboBox.getValue();
        String assignedToFilter = assignedToFilterField.getText().toLowerCase();

        List<TaskDto> filteredTasks = allTasksInGroup.stream()
                .filter(task -> "All".equals(statusFilter) || task.getStatus().equalsIgnoreCase(statusFilter))
                .filter(task -> assignedToFilter.isEmpty() ||
                        (task.getAssignedToName() != null && task.getAssignedToName().toLowerCase().contains(assignedToFilter)))
                .collect(Collectors.toList());

        tasksData.setAll(filteredTasks);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class TaskCell extends ListCell<TaskDto> {
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Label desc = new Label();
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final VBox content = new VBox(6);
        private final HBox actions = new HBox(6);

        TaskCell() {
            actions.getChildren().addAll(editButton, deleteButton);
            content.getChildren().addAll(title, meta, desc, actions);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border; -fx-border-radius: 6; -fx-background-radius: 6;");
            editButton.setOnAction(e -> {
                TaskDto task = getItem();
                if (task != null) handleEditTask(task);
            });
            deleteButton.setOnAction(e -> {
                TaskDto task = getItem();
                if (task != null) handleDeleteTask(task);
            });
        }

        @Override
        protected void updateItem(TaskDto task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
            } else {
                title.setText(task.getTitle() + " (" + task.getStatus() + ")");
                String assignee = task.getAssignedToName() == null ? "Unassigned" : task.getAssignedToName();
                String deadline = task.getDeadline() == null ? "" : " · Due: " + task.getDeadline();
                meta.setText("Assignee: " + assignee + deadline);
                desc.setText(task.getDescription() == null ? "" : task.getDescription());
                setGraphic(content);
            }
        }
    }
}
