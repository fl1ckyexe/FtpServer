package org.example.ftp.admin.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.example.ftp.admin.util.AlertUtil;

public class ServerLimitsView extends BorderPane {

    private final Spinner<Integer> maxConnectionsSpinner =
            new Spinner<>(1, 10_000, 100);

    private final Spinner<Number> uploadLimitSpinner =
            new Spinner<>(0, 1_000_000, 0);

    private final Spinner<Number> downloadLimitSpinner =
            new Spinner<>(0, 1_000_000, 0);

    public ServerLimitsView() {
        setPadding(new Insets(20));
        setTop(createTitle());
        setCenter(createForm());
        setBottom(createActions());
    }

    private Label createTitle() {
        Label label = new Label("Global server limits");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return label;
    }

    private GridPane createForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 0, 20, 0));

        grid.addRow(0,
                new Label("Max connections (global):"),
                maxConnectionsSpinner
        );

        grid.addRow(1,
                new Label("Upload limit (KB/s):"),
                uploadLimitSpinner
        );

        grid.addRow(2,
                new Label("Download limit (KB/s):"),
                downloadLimitSpinner
        );

        return grid;
    }

    private HBox createActions() {
        Button applyBtn = new Button("Apply");
        applyBtn.setOnAction(e -> applyLimits());

        HBox box = new HBox(applyBtn);
        box.setSpacing(10);
        return box;
    }

    private void applyLimits() {

        int maxConn = maxConnectionsSpinner.getValue();

        long upload =
                uploadLimitSpinner.getValue().longValue();

        long download =
                downloadLimitSpinner.getValue().longValue();

        AlertUtil.info(
                "Limits applied:\n" +
                        "Max connections: " + maxConn + "\n" +
                        "Upload: " + upload + " KB/s\n" +
                        "Download: " + download + " KB/s"
        );
    }
}
