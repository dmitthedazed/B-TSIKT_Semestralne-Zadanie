package com.combined.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final double AUTH_WIDTH = 900;
    public static final double AUTH_HEIGHT = 900;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/auth-view.fxml"));
        Scene scene = new Scene(loader.load(), AUTH_WIDTH, AUTH_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("TaskBoard Study Platform - Welcome");
        stage.setWidth(AUTH_WIDTH);
        stage.setHeight(AUTH_HEIGHT);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
