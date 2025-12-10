package com.combined.frontend.controller;

import com.combined.backend.model.Membership; // New import
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

public class AddMemberController {

    @FXML private TextField userEmailField;
    @FXML private TextArea advancedIdentifiersField;
    @FXML private Label errorMessageLabel;

    private ApiService apiService;
    private Long groupId;
    private String groupName;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    @FXML
    private void initialize() {
        errorMessageLabel.setText("");
    }

    public boolean processResult() {
        String single = userEmailField.getText();
        String multi = advancedIdentifiersField != null ? advancedIdentifiersField.getText() : "";

        // Build identifier list (dedup while preserving order)
        Set<String> identifiers = new LinkedHashSet<>();
        if (multi != null && !multi.trim().isEmpty()) {
            for (String part : multi.split("[,\\n]")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) identifiers.add(trimmed);
            }
        } else if (single != null && !single.trim().isEmpty()) {
            identifiers.add(single.trim());
        }

        if (identifiers.isEmpty()) {
            errorMessageLabel.setText("Please enter at least one email/nickname.");
            return false;
        }

        String groupLabel = groupName != null ? groupName : "this group";
        String list = String.join(", ", identifiers);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm add members");
        confirm.setHeaderText("Add members to " + groupLabel + "?");
        confirm.setContentText("Users: " + list + "\n\nThey will be added with role MEMBER.");
        var res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.OK) {
            return false;
        }

        StringBuilder errors = new StringBuilder();
        boolean anySuccess = false;
        StringJoiner added = new StringJoiner(", ");
        for (String id : identifiers) {
            try {
                apiService.addMemberByEmail(groupId, id, Membership.Role.MEMBER);
                anySuccess = true;
                added.add(id);
            } catch (Exception e) {
                if (errors.length() > 0) errors.append("; ");
                errors.append(id).append(": ").append(e.getMessage());
            }
        }

        if (errors.length() > 0) {
            // Show aggregated errors
            errorMessageLabel.setText(errors.toString());
            if (anySuccess) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Partial success");
                warn.setHeaderText("Some users were added, some failed");
                warn.setContentText(errors.toString());
                warn.showAndWait();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Failed to add members");
                err.setHeaderText("No users were added");
                err.setContentText(errors.toString());
                err.showAndWait();
            }
        } else {
            errorMessageLabel.setText("");
        }
        if (anySuccess) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Members added");
            info.setHeaderText("Added to " + groupLabel);
            info.setContentText("Users: " + added);
            info.showAndWait();
        }
        return anySuccess;
    }
}
