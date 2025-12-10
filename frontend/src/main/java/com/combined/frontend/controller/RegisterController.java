package com.combined.frontend.controller;

import com.combined.backend.dto.UserDto;
import com.combined.frontend.MainApp;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private final ApiService apiService = new ApiService();

    private AuthViewController authViewController;

    void setAuthViewController(AuthViewController authViewController) {
        this.authViewController = authViewController;
    }

    @FXML
    private void handleRegister() {
        UserDto userDto = new UserDto();
        userDto.setFirstName(firstNameField.getText());
        userDto.setLastName(lastNameField.getText());
        userDto.setNickname(nicknameField.getText());
        userDto.setEmail(emailField.getText());
        userDto.setPassword(passwordField.getText());
        userDto.setConfirmPassword(confirmPasswordField.getText());

        try {
            apiService.register(userDto);
            showAlert(Alert.AlertType.INFORMATION, "Registration Successful", "Account created. Please sign in.");

            if (authViewController != null) {
                authViewController.showLoginTab(userDto.getEmail());
                return;
            }

            // Fallback: navigate to login screen when not embedded in the auth view
            switchToLogin();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("already exists")) {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "An account with this email or nickname already exists. Please log in or use a different email/nickname.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", message != null ? message : "Unknown error");
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToLogin() {
        if (authViewController != null) {
            authViewController.showLoginTab();
            return;
        }

        try {
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), MainApp.AUTH_WIDTH, MainApp.AUTH_HEIGHT);
            stage.setScene(scene);
            stage.setTitle("Combined Study Platform - Login");
            stage.setWidth(MainApp.AUTH_WIDTH);
            stage.setHeight(MainApp.AUTH_HEIGHT);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load login screen.");
        }
    }

    private void switchToMainView(ApiService apiService, Long userId, String nickname) {
        try {
            Stage stage = (Stage) firstNameField.getScene().getWindow(); // Use any FXML element to get the stage
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);

            MainController mainController = loader.getController();
            mainController.setApiService(apiService);
            mainController.setCurrentUser(userId, nickname);

            stage.setScene(scene);
            stage.setTitle("Combined Study Platform - Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load main view after registration.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
