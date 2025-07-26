module org.example.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.web;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires java.net.http;
    requires com.google.gson;
    requires java.desktop;

    // باز کردن پکیج‌های لازم برای JavaFX و Gson
    opens model to javafx.base, com.google.gson;
    opens controller to javafx.fxml;
    opens view to javafx.fxml;

    // صادرات پکیج‌های مورد نیاز
    exports controller;
    exports view;
    exports org.example.demo1 to javafx.graphics, javafx.controls, javafx.fxml;
}