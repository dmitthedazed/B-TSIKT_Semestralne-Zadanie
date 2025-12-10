package com.combined.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Hosts the Login and Register tabs in a single window and wires up tab switching.
 */
public class AuthViewController {

    @FXML private TabPane authTabPane;
    @FXML private Tab loginTab;
    @FXML private Tab registerTab;

    // Controllers from included FXML files
    @FXML private LoginController loginViewController;
    @FXML private RegisterController registerViewController;

    @FXML
    private void initialize() {
        if (loginViewController != null) {
            loginViewController.setAuthViewController(this);
        }
        if (registerViewController != null) {
            registerViewController.setAuthViewController(this);
        }
    }

    public void showLoginTab() {
        authTabPane.getSelectionModel().select(loginTab);
    }

    public void showLoginTab(String prefillEmail) {
        showLoginTab();
        if (loginViewController != null) {
            loginViewController.prefillEmail(prefillEmail);
        }
    }

    public void showRegisterTab() {
        authTabPane.getSelectionModel().select(registerTab);
    }
}
