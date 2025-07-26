package org.example.demo1;


import view.SignUpView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        Button signUpButton = new Button("Welcome:'>");
        signUpButton.setOnAction(e -> new SignUpView(new Stage()));

        vbox.getChildren().addAll(signUpButton);
        Scene scene = new Scene(vbox, 100, 100);
        primaryStage.setTitle("Food Delivery");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}