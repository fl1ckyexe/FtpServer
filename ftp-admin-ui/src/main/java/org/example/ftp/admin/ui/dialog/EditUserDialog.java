package org.example.ftp.admin.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.example.ftp.admin.dto.UserDto;

import java.util.Optional;

public class EditUserDialog extends Dialog<UserDto> {

    public EditUserDialog(UserDto user) {

        setTitle("Edit user");
        setHeaderText("Edit user: " + user.getUsername());

        ButtonType saveBtnType =
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

        getDialogPane().getButtonTypes().addAll(
                saveBtnType,
                ButtonType.CANCEL
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField(user.getUsername());
        usernameField.setDisable(true);

        CheckBox enabledCheck = new CheckBox("Enabled");
        enabledCheck.setSelected(user.isEnabled());

        Spinner<Long> rateLimitSpinner =
                new Spinner<>(0L, 1_000_000_000L, 0L);
        rateLimitSpinner.setEditable(true);
        rateLimitSpinner.getValueFactory().setValue(user.getUploadLimit() > 0 ? user.getUploadLimit() : 0L);

        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label(""), enabledCheck);
        grid.addRow(2, new Label("Rate limit (bytes/sec, 0 = use server limit):"), rateLimitSpinner);

        getDialogPane().setContent(grid);

        setResultConverter(btn -> {
            if (btn == saveBtnType) {
                user.setEnabled(enabledCheck.isSelected());
                Long rateLimit = rateLimitSpinner.getValue();
                user.setUploadLimit(rateLimit != null && rateLimit > 0 ? rateLimit : 0L);
                return user;
            }
            return null;
        });
    }

    public static Optional<UserDto> show(UserDto user) {
        return new EditUserDialog(user).showAndWait();
    }
}