package view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.UserDTO;
import service.UserService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginView {
    private final UserService authService = new UserService();
    private final Stage stage;

    public LoginView(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(100));
        vbox.setStyle("-fx-background-color: #4CAF50;");

        Label phoneLabel = new Label("Phone:");
        TextField phoneField = new TextField();
        phoneField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");

        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: white;");

        loginButton.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            String password = passwordField.getText().trim();


            CompletableFuture<Map<String, Object>> future = authService.login(phone, password);
            future.thenAccept(response -> {
                Platform.runLater(() -> {
                    System.out.println("Response from server: " + response);
                    if (response != null) {
                        if (response.containsKey("error")) {
                            messageLabel.setText("Error: " + response.get("error"));
                        } else if (response.containsKey("message")) {
                            String message = (String) response.get("message");
                            if (response.containsKey("status") && (int) response.get("status") == 200) {
                                String token = (String) response.get("token");
                                messageLabel.setText("Success: " + message + " (Token: " + token + ")");
                                authService.setToken("Bearer " + token); // ذخیره توکن
                                Map<String, Object> user = (Map<String, Object>) response.get("user");
                                String role = (String) user.get("role");
                                try {

                                      switch (role){
                                          case("buyer"):
                                             MainView mainView = new MainView(stage, token);
                                             break;
                                          case("courier"):
                                             CourierMainView courierMainView = new CourierMainView(stage, token);
                                             break;
                                             case("seller"):
                                                 DashboardView dashboardView = new DashboardView(stage, token);
                                                 break;
                                          default:
                                             break;
                                      }

                                } catch (IOException ex) {
                                    messageLabel.setText("Error loading MainView: " + ex.getMessage());
                                    ex.printStackTrace(); // برای دیدن استثنای کامل توی کنسول
                                }
                            } else {
                                messageLabel.setText(message);
                            }
                        } else {
                            messageLabel.setText("Unexpected response from server! Response: " + response.toString());
                        }
                    } else {
                        messageLabel.setText("No response from server!");
                    }
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    System.out.println("Exception: " + throwable.getMessage());
                    messageLabel.setText("Error: " + throwable.getMessage());
                });
                return null;
            });
        });
        TextField[] fields = { phoneField, passwordField};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    if (index < fields.length - 1) {
                        fields[index + 1].requestFocus();
                    } else {
                        loginButton.requestFocus();
                    }
                }
            });
        }
        // لینک برای بازگشت به ثبت‌نام (اختیاری)
        Hyperlink signUpLink = new Hyperlink("Don't have an account? Sign Up");
        signUpLink.setStyle("-fx-text-fill: white; -fx-underline: true;");
        signUpLink.setOnAction(e -> {
            SignUpView signUpView = new SignUpView(stage);
        });

        vbox.getChildren().addAll(phoneLabel, phoneField, passwordLabel, passwordField, loginButton, signUpLink, messageLabel);

        Scene scene = new Scene(vbox,900,900);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }
}
