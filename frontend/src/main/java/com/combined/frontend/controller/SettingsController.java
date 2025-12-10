package com.combined.frontend.controller;

import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;

public class SettingsController implements BaseController {

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        System.out.println("Settings view loaded.");
    }
}
