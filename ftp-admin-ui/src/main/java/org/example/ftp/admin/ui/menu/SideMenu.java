package org.example.ftp.admin.ui.menu;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.example.ftp.admin.ui.MainView;

public class SideMenu extends VBox {

    public SideMenu(MainView mainView) {
        getStyleClass().add("side-menu"); 
        setSpacing(8);

        Button usersBtn = createMenuButton("Users", e -> mainView.showUsers());
        Button limitsBtn = createMenuButton("Server limits", e -> mainView.showServerLimits());
        Button permissionsBtn = createMenuButton("Permissions", e -> mainView.showPermissions());
        Button statsBtn = createMenuButton("Stats", e -> mainView.showStats());

        
        usersBtn.getStyleClass().add("menu-btn-active");

        getChildren().addAll(usersBtn, limitsBtn, permissionsBtn, statsBtn);
    }

    private Button createMenuButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("menu-btn");
        btn.setOnAction(e -> {
            
            getParent().getChildrenUnmodifiable().forEach(n -> n.getStyleClass().remove("menu-btn-active"));
            btn.getStyleClass().add("menu-btn-active");
            event.handle(e);
        });
        return btn;
    }
}
