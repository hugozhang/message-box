package com.winning.hmap.gui;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: hugo.zxh
 * @date: 2023/09/11 16:22
 */
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VBoxLayoutDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button btnTop = new Button("Top Button");
        Button btnBottom = new Button("Bottom Button");

        VBox vbox = new VBox();
        vbox.getChildren().addAll(btnTop, btnBottom);
        vbox.setAlignment(Pos.TOP_CENTER); // 设置顶部按钮居中
        vbox.setSpacing(20); // 设置按钮之间的垂直间距

        VBox.setVgrow(btnTop, Priority.ALWAYS); // 设置顶部按钮总是填充可用空间
        VBox.setVgrow(btnBottom, Priority.ALWAYS); // 设置底部按钮总是填充可用空间

        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

