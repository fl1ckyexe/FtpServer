package org.example.ftp.admin.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.example.ftp.admin.dto.CreateUserRequestDto;

import java.util.Optional;

public class CreateUserDialog extends Dialog<CreateUserRequestDto> {

    public CreateUserDialog() {
        setTitle("Create user");
        setHeaderText("New FTP user");

        ButtonType createBtnType =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(
                createBtnType,
                ButtonType.CANCEL
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();

        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Password:"), passwordField);

        getDialogPane().setContent(grid);

        Node createBtn = getDialogPane().lookupButton(createBtnType);
        createBtn.setDisable(true);

        Runnable validate = () ->
                createBtn.setDisable(
                        usernameField.getText().trim().isEmpty()
                                || passwordField.getText().trim().isEmpty()
                );

        usernameField.textProperty().addListener((a, b, c) -> validate.run());
        passwordField.textProperty().addListener((a, b, c) -> validate.run());

        setResultConverter(dialogButton -> {
            if (dialogButton == createBtnType) {
                return new CreateUserRequestDto(
                        usernameField.getText().trim(),
                        passwordField.getText()
                );
            }
            return null;
        });
    }

    public static Optional<CreateUserRequestDto> showAndWaitDialog() {
        return new CreateUserDialog().showAndWait();
    }
}