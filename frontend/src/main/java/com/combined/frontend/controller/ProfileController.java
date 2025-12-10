package com.combined.frontend.controller;

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
    @FXML private PasswordField passwordField;
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
        // In a real app, you'd fetch full user details from the backend using currentUserId
        // For now, we'll populate with available info and assume email/nickname are fixed.
        Platform.runLater(() -> {
            // These fields are not editable as per FXML, but we can set them
            nicknameField.setText(currentNickname);
            // emailField.setText(apiService.getCurrentUserEmail()); // If ApiService stored email
            statusLabel.setText("Profile loaded.");
        });
    }

    @FXML
    private void handleUpdateProfile() {
        // TODO: Implement logic to update first name and last name
        showAlert(Alert.AlertType.INFORMATION, "Update Profile", "Update profile functionality coming soon!");
    }

    @FXML
    private void handleChangePassword() {
        // TODO: Implement logic to change password
        showAlert(Alert.AlertType.INFORMATION, "Change Password", "Change password functionality coming soon!");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
