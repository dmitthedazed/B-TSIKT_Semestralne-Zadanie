package com.combined.frontend.controller;

import com.combined.frontend.MainApp;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.service.JwtDecoder;
import com.combined.frontend.service.NotificationService;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private final ApiService apiService = new ApiService();
    private static final String DEFAULT_API = System.getProperty("API_BASE_URL", "http://localhost:8080/api");
    private static final String DEMO_API = "http://localhost:8081/api";

    private AuthViewController authViewController;

    void setAuthViewController(AuthViewController authViewController) {
        this.authViewController = authViewController;
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        apiService.setBaseUrl(DEFAULT_API);
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

        apiService.setBaseUrl(DEFAULT_API);
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
        String password = "demo123";
        emailField.setText(email);
        passwordField.setText(password);
        apiService.setBaseUrl(DEMO_API);
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        try {
            if (email == null || email.isBlank()) {
                showToastError("Please enter your email or username.");
                return;
            }
            if (password == null || password.isBlank()) {
                showToastError("Please enter your password.");
                return;
            }

            apiService.login(email, password);
            String token = apiService.getToken();
            Long userId = JwtDecoder.getUserIdFromToken(token);
            String nickname = JwtDecoder.getNicknameFromToken(token);

            apiService.setTokenAndUserId(token, userId);

            switchToMainView(apiService, userId, nickname);
        } catch (Exception e) {
            showToastError("Wrong username or password.");
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

    private void showToastError(String message) {
        if (emailField == null || emailField.getScene() == null) {
            return;
        }
        Popup popup = new Popup();
        Label label = new Label(message);
        label.setStyle("-fx-background-color: rgba(200,40,40,0.95); -fx-text-fill: white; -fx-padding: 14 18; -fx-background-radius: 10; -fx-font-size: 13px; -fx-font-weight: 600;");
        popup.getContent().add(label);
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        Scene scene = emailField.getScene();
        Stage stage = (Stage) scene.getWindow();

        popup.show(stage);

        // Recompute position after CSS/layout so sizing is accurate
        label.applyCss();
        label.layout();
        javafx.geometry.Bounds rootBounds = scene.getRoot().localToScreen(scene.getRoot().getBoundsInLocal());
        double labelW = label.getWidth();
        double labelH = label.getHeight();
        double x = rootBounds.getMinX() + (rootBounds.getWidth() - labelW) / 2;
        double y = rootBounds.getMaxY() - labelH - 24;
        popup.setX(x);
        popup.setY(y);

        label.setOpacity(0);
        label.setTranslateY(20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), label);
        slideUp.setFromY(20);
        slideUp.setToY(0);
        ParallelTransition showAnim = new ParallelTransition(fadeIn, slideUp);

        PauseTransition stay = new PauseTransition(Duration.seconds(2.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(200), label);
        slideDown.setFromY(0);
        slideDown.setToY(10);
        ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideDown);
        hideAnim.setOnFinished(e -> popup.hide());

        showAnim.setOnFinished(e -> stay.play());
        stay.setOnFinished(e -> hideAnim.play());
        showAnim.play();
    }

    // Allow parent to prefill the login email after registration
    void prefillEmail(String email) {
        if (emailField != null && email != null) {
            emailField.setText(email);
        }
    }
}
