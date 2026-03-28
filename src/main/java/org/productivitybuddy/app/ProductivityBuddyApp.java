package org.productivitybuddy.app;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.productivitybuddy.ProductivityBuddyLauncher;

public class ProductivityBuddyApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(ProductivityBuddyLauncher.class.getResource("main-view.fxml"));
        final Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.show();
    }
}
