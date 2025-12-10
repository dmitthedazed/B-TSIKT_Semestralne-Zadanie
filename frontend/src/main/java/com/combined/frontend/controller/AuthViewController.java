package com.combined.frontend.controller;

import javafx.fxml.FXML;

/**
 * Hosts the authentication view. Register tab is currently disabled in the UI;
 * keep the controller lean until multi-tab is reintroduced.
 */
public class AuthViewController {

    @FXML private javafx.scene.layout.Region loginView;
    @FXML private javafx.scene.layout.Region registerView;
    @FXML private LoginController loginViewController;
    @FXML private RegisterController registerViewController;

    @FXML
    private void initialize() {
        if (loginViewController != null) {
            loginViewController.setAuthViewController(this);
        }
        // Register is not shown in the current UI, but keep wiring if present
        if (registerViewController != null) {
            registerViewController.setAuthViewController(this);
        }
        // Default to showing login
        showLoginTab();
    }

    public void showLoginTab() {
        if (loginView != null) {
            loginView.setVisible(true);
            loginView.setManaged(true);
        }
        if (registerView != null) {
            registerView.setVisible(false);
            registerView.setManaged(false);
        }
    }

    public void showLoginTab(String prefillEmail) {
        showLoginTab();
        if (loginViewController != null) {
            loginViewController.prefillEmail(prefillEmail);
        }
    }

    public void showRegisterTab() {
        if (registerView != null) {
            registerView.setVisible(true);
            registerView.setManaged(true);
        }
        if (loginView != null) {
            loginView.setVisible(false);
            loginView.setManaged(false);
        }
    }
}
