package view;
import controller.CourierMainViewController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class CourierMainView {
    private final Stage stage;
    private final String token;

    public CourierMainView(Stage stage, String token) throws IOException {
        this.stage = stage;
        this.token = token;
        setupUI();
    }

    private void setupUI() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/courierMainView.fxml"));
        // ایجاد دستی کنترلر با پارامترهای stage و token
        CourierMainViewController controller = new CourierMainViewController(stage, token);
        loader.setController(controller); // تنظیم کنترلر
        Parent root = loader.load();
        stage.setScene(new Scene(root, 2000, 900));
        stage.setTitle("Courier Dashboard");
        stage.show();
    }
}