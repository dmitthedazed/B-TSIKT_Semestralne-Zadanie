package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateGroupController {

    @FXML private TextField groupNameField;
    @FXML private TextArea groupDescriptionField;
    @FXML private Label errorMessageLabel;

    private ApiService apiService;
    private GroupDto newGroup;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public GroupDto getNewGroup() {
        return newGroup;
    }

    public String getLastErrorMessage() {
        return errorMessageLabel == null ? "" : errorMessageLabel.getText();
    }

    @FXML
    private void initialize() {
        errorMessageLabel.setText("");
    }

    public boolean processResult() {
        String name = groupNameField.getText();
        String description = groupDescriptionField.getText();

        if (name == null || name.trim().isEmpty()) {
            errorMessageLabel.setText("Group name cannot be empty.");
            return false;
        }
        if (description == null || description.trim().isEmpty()) {
            errorMessageLabel.setText("Group description cannot be empty.");
            return false;
        }
        if (description.trim().length() < 5 || description.trim().length() > 300) {
            errorMessageLabel.setText("Description must be between 5 and 300 characters.");
            return false;
        }

        try {
            GroupDto groupToCreate = new GroupDto();
            groupToCreate.setName(name);
            groupToCreate.setDescription(description);

            newGroup = apiService.createGroup(groupToCreate);
            return true;
        } catch (Exception e) {
            errorMessageLabel.setText("Failed to create group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
