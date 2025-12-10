package com.combined.frontend.controller;

import com.combined.backend.dto.TaskDto;
import com.combined.frontend.dto.MemberWithRoleDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform; // New import
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Control;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class CreateTaskController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker deadlinePicker;
    @FXML private ComboBox<MemberWithRoleDto> assignedToComboBox; // New ComboBox for assigned user
    @FXML private Label errorMessageLabel;

    private ApiService apiService;
    private MainController mainController;
    private Long groupId;
    private TaskDto newTask;
    private TaskDto taskToEdit; // To hold the task being edited
    private boolean saved;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
        loadGroupMembers(); // Load members when group ID is set
    }

    public void setTaskToEdit(TaskDto task) {
        this.taskToEdit = task;
        // Pre-fill fields if editing an existing task
        if (task != null) {
            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription());
            statusComboBox.getSelectionModel().select(task.getStatus());
            if (task.getDeadline() != null) {
                deadlinePicker.setValue(task.getDeadline().toLocalDate());
            }
            // Select assigned user in ComboBox
            if (task.getAssignedToId() != null) {
                // Ensure members are loaded before trying to select
                // This might need to be handled asynchronously or after members are guaranteed to be loaded
                assignedToComboBox.getItems().stream()
                        .filter(user -> user != null && user.getUserId() != null && user.getUserId().equals(task.getAssignedToId()))
                        .findFirst()
                        .ifPresent(user -> assignedToComboBox.getSelectionModel().select(user));
            }
        }
    }

    public TaskDto getNewTask() {
        return newTask;
    }

    public String getLastErrorMessage() {
        return errorMessageLabel == null ? "" : errorMessageLabel.getText();
    }

    public boolean wasSaved() {
        return saved;
    }

    @FXML
    private void initialize() {
        errorMessageLabel.setText("");
        statusComboBox.setItems(FXCollections.observableArrayList("OPEN", "IN_PROGRESS", "DONE", "CLOSED"));
        statusComboBox.getSelectionModel().selectFirst(); // Default status

        // Custom cell factory for assignedToComboBox to display nickname
        if (assignedToComboBox != null) {
            assignedToComboBox.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(MemberWithRoleDto user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? "" : user.getNickname());
                }
            });
            assignedToComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(MemberWithRoleDto user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? "" : user.getNickname());
                }
            });
        }
    }

    private void loadGroupMembers() {
        if (apiService == null || groupId == null || assignedToComboBox == null) {
            return;
        }
        new Thread(() -> {
            try {
                List<MemberWithRoleDto> members = apiService.getGroupMembers(groupId); // Now returns DTO with role
                Platform.runLater(() -> {
                    assignedToComboBox.getItems().add(null); // Option for no assignee
                    assignedToComboBox.getItems().addAll(members);
                    // If editing, try to select the assigned user again after loading members
                    if (taskToEdit != null && taskToEdit.getAssignedToId() != null) {
                        assignedToComboBox.getItems().stream()
                                .filter(user -> user != null && user.getUserId() != null && user.getUserId().equals(taskToEdit.getAssignedToId()))
                                .findFirst()
                                .ifPresent(user -> assignedToComboBox.getSelectionModel().select(user));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessageLabel.setText("Failed to load group members: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    public boolean processResult() { // For creating a new task
        return saveTask(null);
    }

    public boolean processEditResult(Long taskId) { // For editing an existing task
        return saveTask(taskId);
    }

    private boolean saveTask(Long taskId) {
        saved = false;
        clearValidationStyles();
        String title = titleField.getText();
        String description = descriptionField.getText();
        String status = statusComboBox.getValue();
        LocalDate deadlineDate = deadlinePicker.getValue();
        MemberWithRoleDto assignedTo = assignedToComboBox.getValue();

        if (title == null || title.trim().isEmpty()) {
            markInvalid(titleField, "Task title cannot be empty.");
            return false;
        }
        if (title.trim().length() < 3 || title.trim().length() > 150) {
            markInvalid(titleField, "Title must be between 3 and 150 characters.");
            return false;
        }
        if (description != null && description.trim().length() > 500) {
            markInvalid(descriptionField, "Description cannot exceed 500 characters.");
            return false;
        }
        if (status == null || status.trim().isEmpty()) {
            markInvalid(statusComboBox, "Task status cannot be empty.");
            return false;
        }
        if (deadlineDate == null) {
            markInvalid(deadlinePicker, "Task deadline cannot be empty.");
            return false;
        }

        try {
            TaskDto taskToSave = new TaskDto();
            taskToSave.setGroupId(groupId);
            taskToSave.setTitle(title);
            taskToSave.setDescription(description);
            taskToSave.setStatus(status);
            taskToSave.setDeadline(LocalDateTime.of(deadlineDate, LocalTime.MAX));
            taskToSave.setAssignedToId(assignedTo != null ? assignedTo.getUserId() : null);

            if (taskId == null) { // Create
                newTask = apiService.createTask(taskToSave);
            } else { // Update
                newTask = apiService.updateTask(taskId, taskToSave);
            }
            saved = true;
            return true;
        } catch (Exception e) {
            errorMessageLabel.setText("Failed to save task: " + e.getMessage());
            if (mainController != null) {
                mainController.showToast("Failed to save task: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    private void clearValidationStyles() {
        if (errorMessageLabel != null) errorMessageLabel.setText("");
        resetStyle(titleField);
        resetStyle(descriptionField);
        resetStyle(statusComboBox);
        resetStyle(deadlinePicker);
    }

    private void resetStyle(Control control) {
        if (control != null) control.setStyle("");
    }

    private void markInvalid(Control control, String message) {
        if (control != null) {
            control.setStyle("-fx-border-color: #d9534f; -fx-border-radius: 4; -fx-background-radius: 4;");
            control.requestFocus();
        }
        if (errorMessageLabel != null) errorMessageLabel.setText(message);
        if (mainController != null) {
            mainController.showToast(message);
        }
    }
}
