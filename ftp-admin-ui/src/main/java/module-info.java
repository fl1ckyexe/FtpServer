module org.example.ftp.admin {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports org.example.ftp.admin;
    exports org.example.ftp.admin.ui;
    exports org.example.ftp.admin.ui.menu;
    exports org.example.ftp.admin.ui.view;
    exports org.example.ftp.admin.ui.dialog;
    exports org.example.ftp.admin.dto;

    opens org.example.ftp.admin to javafx.graphics;
}
