package com.combined.frontend.controller;

import com.combined.backend.dto.PasswordChangeDto;
import com.combined.backend.dto.ProfileUpdateDto;
import com.combined.backend.dto.UserDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController implements BaseController { // Implement BaseController

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private TextField userIdField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;

    @Override // Implement BaseController method
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override // Implement BaseController method
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        loadUserProfile();
    }

    @Override // Implement BaseController method
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    private void loadUserProfile() {
        if (apiService == null || currentUserId == null) {
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }
        new Thread(() -> {
            try {
                UserDto user = apiService.getCurrentUser();
                Platform.runLater(() -> {
                    firstNameField.setText(user.getFirstName());
                    lastNameField.setText(user.getLastName());
                    nicknameField.setText(user.getNickname());
                    emailField.setText(user.getEmail());
                    userIdField.setText(user.getId() != null ? user.getId().toString() : "");
                    statusLabel.setStyle("-fx-text-fill: #16a34a;");
                    statusLabel.setText("Profile loaded.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Failed to load profile: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleUpdateProfile() {
        String firstName = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String lastName = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        if (firstName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Update Profile", "Name cannot be empty.");
            return;
        }
        ProfileUpdateDto dto = new ProfileUpdateDto(firstName, lastName);
        new Thread(() -> {
            try {
                UserDto updated = apiService.updateProfile(dto);
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: #16a34a;");
                    statusLabel.setText("Profile updated.");
                    firstNameField.setText(updated.getFirstName());
                    lastNameField.setText(updated.getLastName());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Failed to update profile: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleChangePassword() {
        String current = currentPasswordField.getText();
        String np = newPasswordField.getText();
        String cp = confirmPasswordField.getText();
        if (current == null || current.isEmpty() || np == null || np.isEmpty() || cp == null || cp.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Change password", "All password fields are required.");
            return;
        }
        if (!np.equals(cp)) {
            showAlert(Alert.AlertType.WARNING, "Change password", "New passwords do not match.");
            return;
        }
        if (np.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Change password", "Password must be at least 6 characters.");
            return;
        }
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(np);
        dto.setConfirmPassword(cp);

        new Thread(() -> {
            try {
                apiService.changePassword(dto);
                Platform.runLater(() -> {
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    statusLabel.setStyle("-fx-text-fill: #16a34a;");
                    statusLabel.setText("Password changed successfully.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Failed to change password: " + e.getMessage());
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
