package com.combined.frontend.controller;

import com.combined.frontend.MainApp;
import com.combined.frontend.controller.AuthViewController;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.service.JwtDecoder;
import com.combined.frontend.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private final ApiService apiService = new ApiService();

    private AuthViewController authViewController;

    void setAuthViewController(AuthViewController authViewController) {
        this.authViewController = authViewController;
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        performLogin(email, password);
    }

    @FXML
    private void handleAutoLogin() {
        // Test user credentials
        String email = "test@example.com";
        String password = "password123";

        // Optionally fill the fields for visual feedback
        emailField.setText(email);
        passwordField.setText(password);

        performLogin(email, password);
    }

    @FXML
    private void handleAutoLoginOccaecat() {
        String email = "dmitriyuzat@gmail.com";
        String password = "REDACTED";
        emailField.setText(email);
        passwordField.setText(password);
        performLogin(email, password);
    }

    @FXML
    private void handleTryDemo() {
        // Demo account (fake data) — adjust credentials if your backend provides a dedicated demo user
        String email = "demo@example.com";
        String password = "demo1234";
        emailField.setText(email);
        passwordField.setText(password);
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        try {
            if (email == null || email.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Please enter your email or username.");
                return;
            }
            if (password == null || password.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Please enter your password.");
                return;
            }

            apiService.login(email, password);
            String token = apiService.getToken();
            Long userId = JwtDecoder.getUserIdFromToken(token);
            String nickname = JwtDecoder.getNicknameFromToken(token);

            apiService.setTokenAndUserId(token, userId);

            showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome back, " + nickname + "!");
            switchToMainView(apiService, userId, nickname);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Login Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToRegister() {
        if (authViewController != null) {
            authViewController.showRegisterTab();
        }
    }

    private void switchToMainView(ApiService apiService, Long userId, String nickname) {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);

            MainController mainController = loader.getController();
            mainController.setApiService(apiService);
            NotificationService notificationService = new NotificationService();
            notificationService.setCurrentUserId(userId);
            notificationService.connect();
            mainController.setNotificationService(notificationService);
            mainController.setCurrentUser(userId, nickname);

            stage.setScene(scene);
            stage.setTitle("Combined Study Platform - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load main view.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Allow parent to prefill the login email after registration
    void prefillEmail(String email) {
        if (emailField != null && email != null) {
            emailField.setText(email);
        }
    }
}
