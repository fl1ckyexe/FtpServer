package org.example.ftp.admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.ftp.admin.ui.MainView;

public class AdminApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("FTP Admin Panel");
        Scene scene = new Scene(new MainView(), 900, 500);

        String css = getClass().getResource("/styles/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
