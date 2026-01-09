package org.example.ftp.admin.ui.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.example.ftp.admin.api.AdminApiClient;
import org.example.ftp.admin.dto.UserStatsDto;
import org.example.ftp.admin.util.AlertUtil;

public class StatsView extends BorderPane {

    private final TableView<UserStatsDto> table = new TableView<>();
    private final AdminApiClient api;

    public StatsView(AdminApiClient api) {
        this.api = api;

        setPadding(new Insets(10));
        setTop(createToolbar());
        setCenter(createTable());

        refresh();
    }

    private Node createToolbar() {
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("action-btn"); 

        refreshBtn.setOnAction(e -> refresh());

        HBox box = new HBox(10, refreshBtn);
        box.setPadding(new Insets(15, 5, 15, 5)); 
        return box;
    }

    private Node createTable() {

        TableColumn<UserStatsDto, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(160);

        TableColumn<UserStatsDto, Integer> loginsCol = new TableColumn<>("Logins");
        loginsCol.setCellValueFactory(new PropertyValueFactory<>("logins"));
        loginsCol.setPrefWidth(90);

        TableColumn<UserStatsDto, Long> upCol = new TableColumn<>("Uploaded");
        upCol.setCellValueFactory(new PropertyValueFactory<>("bytesUploaded"));
        upCol.setPrefWidth(130);

        TableColumn<UserStatsDto, Long> downCol = new TableColumn<>("Downloaded");
        downCol.setCellValueFactory(new PropertyValueFactory<>("bytesDownloaded"));
        downCol.setPrefWidth(130);

        TableColumn<UserStatsDto, String> lastCol = new TableColumn<>("Last login");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
        lastCol.setPrefWidth(220);

        table.getColumns().addAll(userCol, loginsCol, upCol, downCol, lastCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return table;
    }


    private void refresh() {
        table.setPlaceholder(new Label("Loading..."));
        new Thread(() -> {
            try {
                var data = api.getStats();
                Platform.runLater(() -> {
                    table.getItems().setAll(data);
                    table.setPlaceholder(new Label("No data"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    table.setPlaceholder(new Label("No data"));
                    AlertUtil.error("Failed to load stats: " + e.getMessage());
                });
            }
        }, "stats-loader").start();
    }

}
