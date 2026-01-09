package org.example.ftp.admin.util;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public final class AlertUtil {

    private AlertUtil() {
    }

    public static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDarkTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDarkTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    private static void applyDarkTheme(DialogPane pane) {
        String css = AlertUtil.class.getResource("/styles/style.css").toExternalForm();
        if (!pane.getStylesheets().contains(css)) {
            pane.getStylesheets().add(css);
        }
    }
}