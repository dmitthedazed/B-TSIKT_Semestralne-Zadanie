package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Control;

public class CreateGroupController {

    @FXML private TextField groupNameField;
    @FXML private TextArea groupDescriptionField;
    @FXML private Label errorMessageLabel;

    private ApiService apiService;
    private MainController mainController;
    private GroupDto newGroup;
    private boolean saved;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public GroupDto getNewGroup() {
        return newGroup;
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
    }

    public boolean processResult() {
        saved = false;
        clearValidation();
        String name = groupNameField.getText();
        String description = groupDescriptionField.getText();

        if (name == null || name.trim().isEmpty()) {
            markInvalid(groupNameField, "Group name cannot be empty.");
            return false;
        }
        if (description == null || description.trim().isEmpty()) {
            markInvalid(groupDescriptionField, "Group description cannot be empty.");
            return false;
        }
        if (description.trim().length() < 5 || description.trim().length() > 300) {
            markInvalid(groupDescriptionField, "Description must be between 5 and 300 characters.");
            return false;
        }

        try {
            GroupDto groupToCreate = new GroupDto();
            groupToCreate.setName(name);
            groupToCreate.setDescription(description);

            newGroup = apiService.createGroup(groupToCreate);
            saved = true;
            return true;
        } catch (Exception e) {
            String msg = "Failed to create group: " + e.getMessage();
            errorMessageLabel.setText(msg);
            if (mainController != null) {
                mainController.showToast(msg);
            }
            e.printStackTrace();
            return false;
        }
    }

    private void clearValidation() {
        if (errorMessageLabel != null) errorMessageLabel.setText("");
        resetStyle(groupNameField);
        resetStyle(groupDescriptionField);
    }

    private void resetStyle(Control c) {
        if (c != null) c.setStyle("");
    }

    private void markInvalid(Control c, String message) {
        if (c != null) {
            c.setStyle("-fx-border-color: #d9534f; -fx-border-radius: 4; -fx-background-radius: 4;");
            c.requestFocus();
        }
        if (errorMessageLabel != null) errorMessageLabel.setText(message);
        if (mainController != null) {
            mainController.showToast(message);
        }
    }
}
