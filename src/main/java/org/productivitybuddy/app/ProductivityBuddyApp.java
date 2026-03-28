package org.productivitybuddy.app;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.productivitybuddy.ProductivityBuddyLauncher;
import org.productivitybuddy.config.ApplicationSpringConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ProductivityBuddyApp extends Application {
    private AnnotationConfigApplicationContext springContext;

    @Override
    public void init() {
        this.springContext = new AnnotationConfigApplicationContext(ApplicationSpringConfig.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(ProductivityBuddyLauncher.class.getResource("main-view.fxml"));
        fxmlLoader.setControllerFactory(this.springContext::getBean);

        final Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        this.springContext.close();
    }
}
