package com.winning.hmap.gui;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: hugo.zxh
 * @date: 2023/09/11 16:23
 */
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HBoxLayoutDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HBox布局示例");

        HBox hbox = new HBox();
        Button button1 = new Button("按钮1");
        Button button2 = new Button("按钮2");
        Button button3 = new Button("按钮3");
        hbox.setSpacing(100);
        hbox.getChildren().addAll(button1, button2, button3);

        Scene scene = new Scene(hbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

