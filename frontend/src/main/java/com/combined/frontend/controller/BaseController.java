package com.combined.frontend.controller;

import com.combined.frontend.service.ApiService;

public interface BaseController {
    void setApiService(ApiService apiService);
    void setCurrentUser(Long userId, String nickname);
    void setMainController(MainController mainController);
}
