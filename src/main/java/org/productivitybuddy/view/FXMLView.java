package org.productivitybuddy.view;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.productivitybuddy.ProductivityBuddyLauncher;
import org.springframework.context.ApplicationContext;

public final class FXMLView<T> {
    private static final String BASE_PATH = "/org/productivitybuddy/";

    private final Node view;
    private final T controller;

    public FXMLView(Node view, T controller) {
        this.view = view;
        this.controller = controller;
    }

    /**
     * Loads an FXML view from the default application FXML directory and returns
     * both the root node and the instantiated controller.
     *
     * @param resourcePath FXML file name or relative path under {@code /org/productivitybuddy/}
     * @param context Spring application context used to create controllers
     * @param <T> controller type
     * @return loaded FXML view and controller pair
     */
    public static <T> FXMLView<T> loadView(String resourcePath, ApplicationContext context) {
        final String normalizedPath = normalizeResourcePath(resourcePath);

        try {
            final FXMLLoader loader = new FXMLLoader(ProductivityBuddyLauncher.class.getResource(normalizedPath));
            loader.setControllerFactory(context::getBean);
            return new FXMLView<>(loader.load(), loader.getController());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + normalizedPath, e);
        }
    }

    public Node getView() {
        return this.view;
    }

    public T getController() {
        return this.controller;
    }

    private static String normalizeResourcePath(String resourcePath) {
        final String relativePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (relativePath.startsWith("org/productivitybuddy/")) {
            return "/" + relativePath;
        }

        return BASE_PATH + relativePath;
    }
}
