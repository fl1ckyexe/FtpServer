package org.example.ftp.admin.ui.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.ftp.admin.api.AdminApiClient;
import org.example.ftp.admin.dto.FolderPermissionDto;
import org.example.ftp.admin.dto.UserDto;
import org.example.ftp.admin.dto.UserPermissionsDto;
import org.example.ftp.admin.util.AlertUtil;

import java.io.IOException;

public class PermissionsView extends BorderPane {

    private final AdminApiClient api;

    private final ComboBox<UserDto> userBox = new ComboBox<>();

    private final CheckBox globalR = new CheckBox("Global R");
    private final CheckBox globalW = new CheckBox("Global W");
    private final CheckBox globalE = new CheckBox("Global E");

    private final Button refreshBtn = new Button("Refresh");
    private final Button saveBtn = new Button("Save");
    private final Button confirmBtn = new Button("Confirm");

    private final TableView<FolderPermissionDto> table = new TableView<>();

    public PermissionsView(AdminApiClient api) {
        this.api = api;

        setPadding(new Insets(20));
        setTop(createTop());
        setCenter(createTable());
        setBottom(createActions());

        initUserBox();
        loadUsers();

        setControlsEnabled(false);
    }

    private VBox createTop() {
        Label userLabel = new Label("User:");

        userBox.setPrefWidth(260);
        userBox.setOnAction(e -> onUserSelected());

        refreshBtn.setOnAction(e -> refreshSelectedUser());

        HBox userRow = new HBox(10, userLabel, userBox, refreshBtn);
        userRow.setPadding(new Insets(0, 0, 10, 0));

        HBox globalRow = new HBox(12, globalR, globalW, globalE);
        globalRow.setPadding(new Insets(0, 0, 10, 0));

        return new VBox(0, userRow, globalRow);
    }

    private void setControlsEnabled(boolean enabled) {
        userBox.setDisable(false); 
        refreshBtn.setDisable(!enabled);
        saveBtn.setDisable(!enabled);
        confirmBtn.setDisable(!enabled);

        globalR.setDisable(!enabled);
        globalW.setDisable(!enabled);
        globalE.setDisable(!enabled);

        table.setDisable(!enabled);
    }

    private TableView<FolderPermissionDto> createTable() {
        table.setEditable(true);

        TableColumn<FolderPermissionDto, String> folderCol = new TableColumn<>("Folder");
        folderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFolder()));
        folderCol.setPrefWidth(340);

        TableColumn<FolderPermissionDto, Boolean> rCol =
                createCheckColumn("R", FolderPermissionDto::isRead, FolderPermissionDto::setRead);

        TableColumn<FolderPermissionDto, Boolean> wCol =
                createCheckColumn("W", FolderPermissionDto::isWrite, FolderPermissionDto::setWrite);

        TableColumn<FolderPermissionDto, Boolean> eCol =
                createCheckColumn("E", FolderPermissionDto::isExecute, FolderPermissionDto::setExecute);

        table.getColumns().setAll(folderCol, rCol, wCol, eCol);
        table.setPlaceholder(new Label("Select user to load permissions"));

        return table;
    }

    private TableColumn<FolderPermissionDto, Boolean> createCheckColumn(
            String title,
            java.util.function.Function<FolderPermissionDto, Boolean> getter,
            java.util.function.BiConsumer<FolderPermissionDto, Boolean> setter
    ) {
        TableColumn<FolderPermissionDto, Boolean> col = new TableColumn<>(title);

        col.setCellValueFactory(c -> {
            FolderPermissionDto dto = c.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(getter.apply(dto));
            prop.addListener((obs, oldV, newV) -> setter.accept(dto, newV));
            return prop;
        });

        col.setCellFactory(CheckBoxTableCell.forTableColumn(col));
        col.setEditable(true);
        col.setPrefWidth(70);

        return col;
    }

    private HBox createActions() {
        saveBtn.setOnAction(e -> saveAll());
        confirmBtn.setOnAction(e -> confirmAll());

        HBox box = new HBox(10, saveBtn, confirmBtn);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void initUserBox() {
        userBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(UserDto user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getUsername());
            }
        });

        userBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UserDto user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? "" : user.getUsername());
            }
        });
    }

    private void loadUsers() {
        try {
            userBox.setItems(FXCollections.observableArrayList(api.getUsers()));
            table.setPlaceholder(new Label("Select user to load permissions"));
        } catch (Exception e) {
            table.setPlaceholder(new Label("No data"));
            AlertUtil.error("Failed to load users: " + e.getMessage());
        }
    }

    private void onUserSelected() {
        if (userBox.getValue() == null) {
            clearView();
            setControlsEnabled(false);
            return;
        }
        setControlsEnabled(true);
        refreshSelectedUser();
    }

    private void refreshSelectedUser() {
        UserDto user = userBox.getValue();
        if (user == null) return;

        setControlsEnabled(false);
        table.setPlaceholder(new Label("Loading..."));

        new Thread(() -> {
            try {
                UserPermissionsDto gp;
                try {
                    gp = api.getUserPermissions(user.getUsername());
                } catch (IOException ex) {
                    gp = new UserPermissionsDto(user.getUsername(), false, false, false);
                }

                var folderPerms = api.getFolderPermissions(user.getUsername());

                UserPermissionsDto finalGp = gp;
                Platform.runLater(() -> {
                    globalR.setSelected(finalGp.isRead());
                    globalW.setSelected(finalGp.isWrite());
                    globalE.setSelected(finalGp.isExecute());

                    table.setItems(FXCollections.observableArrayList(folderPerms));
                    table.setPlaceholder(new Label(folderPerms.isEmpty() ? "No folder permissions" : ""));
                    setControlsEnabled(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearView();
                    setControlsEnabled(true);
                    AlertUtil.error("Failed to load permissions: " + e.getMessage());
                });
            }
        }, "permissions-loader").start();
    }

    private void saveAll() {
        UserDto user = userBox.getValue();
        if (user == null) {
            AlertUtil.error("Select user first");
            return;
        }

        setControlsEnabled(false);

        new Thread(() -> {
            try {
                api.savePermissions(
                        user.getUsername(),
                        globalR.isSelected(),
                        globalW.isSelected(),
                        globalE.isSelected()
                );

                for (FolderPermissionDto p : table.getItems()) {
                    api.saveFolderPermission(
                            user.getUsername(),
                            p.getFolder(),
                            p.isRead(),
                            p.isWrite(),
                            p.isExecute()
                    );
                }

                Platform.runLater(() -> {
                    setControlsEnabled(true);
                    AlertUtil.info("Permissions saved");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setControlsEnabled(true);
                    AlertUtil.error("Failed to save permissions: " + e.getMessage());
                });
            }
        }, "permissions-saver").start();
    }

    private void confirmAll() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to confirm and save changes?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                saveAll();
            }
        });
    }

    private void clearView() {
        globalR.setSelected(false);
        globalW.setSelected(false);
        globalE.setSelected(false);
        table.getItems().clear();
        table.setPlaceholder(new Label("Select user to load permissions"));
    }
}