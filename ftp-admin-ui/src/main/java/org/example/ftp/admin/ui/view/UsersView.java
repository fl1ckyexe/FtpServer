package org.example.ftp.admin.ui.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.example.ftp.admin.api.AdminApiClient;
import org.example.ftp.admin.dto.UserDto;
import org.example.ftp.admin.ui.dialog.CreateUserDialog;
import org.example.ftp.admin.ui.dialog.EditUserDialog;
import org.example.ftp.admin.util.AlertUtil;

public class UsersView extends BorderPane {

    private final TableView<UserDto> table = new TableView<>();
    private final AdminApiClient api;

    public UsersView(AdminApiClient api) {
        this.api = api;

        setPadding(new Insets(10));
        setTop(createToolbar());
        setCenter(createTable());

        loadUsersFromApi();
    }

    private Node createToolbar() {
        Button createBtn = new Button("Create user");
        Button deleteBtn = new Button("Delete user");
        Button refreshBtn = new Button("Refresh");

        createBtn.setOnAction(e -> openCreateDialog());
        deleteBtn.setOnAction(e -> deleteSelectedUser());
        refreshBtn.setOnAction(e -> loadUsersFromApi());

        HBox box = new HBox(10, createBtn, deleteBtn, refreshBtn);
        box.setPadding(new Insets(5));
        return box;
    }

    private Node createTable() {

        TableColumn<UserDto, String> usernameCol =
                new TableColumn<>("Username");
        usernameCol.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );
        usernameCol.setPrefWidth(220);

        TableColumn<UserDto, Boolean> enabledCol =
                new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(
                new PropertyValueFactory<>("enabled")
        );
        enabledCol.setPrefWidth(120);

        table.getColumns().setAll(usernameCol, enabledCol);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        table.setRowFactory(tv -> {
            TableRow<UserDto> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editUser(row.getItem());
                }
            });

            return row;
        });

        return table;
    }

    private void loadUsersFromApi() {
        try {
            table.getItems().setAll(api.getUsers());
        } catch (Exception e) {
            AlertUtil.error("Failed to load users: " + e.getMessage());
        }
    }

    private void openCreateDialog() {
        CreateUserDialog.showAndWaitDialog().ifPresent(req -> {
            try {
                api.createUser(req);
                loadUsersFromApi();
            } catch (Exception e) {
                AlertUtil.error("Failed to create user: " + e.getMessage());
            }
        });
    }

    private void editUser(UserDto user) {
        EditUserDialog.show(user).ifPresent(updated -> {
            try {
                Long rateLimit = updated.getUploadLimit() > 0 ? updated.getUploadLimit() : null;
                api.updateUser(updated.getUsername(), updated.isEnabled(), rateLimit);
                loadUsersFromApi();
            } catch (Exception e) {
                AlertUtil.error("Failed to update user: " + e.getMessage());
            }
        });
    }

    private void deleteSelectedUser() {
        UserDto selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            AlertUtil.error("Select user first");
            return;
        }

        try {
            api.deleteUser(selected.getUsername());
            table.getItems().remove(selected);
        } catch (Exception e) {
            AlertUtil.error("Failed to delete user: " + e.getMessage());
        }
    }
}