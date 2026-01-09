package org.example.ftp.admin.ui;

import javafx.scene.layout.BorderPane;
import org.example.ftp.admin.api.AdminApiClient;
import org.example.ftp.admin.ui.menu.SideMenu;
import org.example.ftp.admin.ui.view.PermissionsView;
import org.example.ftp.admin.ui.view.ServerLimitsView;
import org.example.ftp.admin.ui.view.StatsView;
import org.example.ftp.admin.ui.view.UsersView;

public class MainView extends BorderPane {

    private final AdminApiClient api = new AdminApiClient();


    public MainView() {
        setLeft(new SideMenu(this));
        setCenter(new UsersView(api));

    }

    public void showUsers() {
        setCenter(new UsersView(api));
    }

    public void showServerLimits() {
        setCenter(new ServerLimitsView());
    }

    public void showPermissions() {
        setCenter(new PermissionsView(api));
    }

    public void showStats() {
        setCenter(new StatsView(api));
    }

}
