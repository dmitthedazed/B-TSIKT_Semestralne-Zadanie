package com.combined.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/auth-view.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setScene(scene);
        stage.setTitle("Combined Study Platform - Welcome");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
