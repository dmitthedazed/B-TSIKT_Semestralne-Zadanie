package com.combined.frontend.controller;

import com.combined.frontend.service.ApiService;
import com.combined.frontend.service.NotificationAware;
import com.combined.frontend.service.NotificationService;
import com.combined.frontend.service.NotificationService.NotificationMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class NotificationCenterController implements BaseController, NotificationAware {

    @FXML private ListView<String> notificationsList;
    @FXML private Label statusLabel;
    @FXML private Button clearButton;

    private final ObservableList<String> items = FXCollections.observableArrayList();
    private NotificationService notificationService;
    private final Consumer<NotificationMessage> handler = this::onNotification;

    @Override
    public void setApiService(ApiService apiService) {
        // not used here
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        // not used here
    }

    @Override
    public void setMainController(MainController mainController) {
        // not used
    }

    @Override
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        if (notificationService != null) {
            notificationService.registerGlobalHandler(handler);
        }
    }

    @FXML
    private void initialize() {
        notificationsList.setItems(items);
        notificationsList.setPlaceholder(new Label("No notifications yet."));
        statusLabel.setText("Listening for updates...");
    }

    @FXML
    private void handleClear() {
        items.clear();
    }

    private void onNotification(NotificationMessage msg) {
        if (msg == null) return;
        String ts = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(msg.getTimestamp()));
        String line = "[" + ts + "] " + (msg.getMessage() != null ? msg.getMessage() : msg.getType());
        Platform.runLater(() -> {
            items.add(0, line);
            statusLabel.setText("Last update: " + ts);
        });
    }
}
