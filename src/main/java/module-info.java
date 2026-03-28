module org.productivitybuddy.productivitybuddy {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;

    opens org.productivitybuddy to javafx.fxml;
    exports org.productivitybuddy;
    exports org.productivitybuddy.app;
    opens org.productivitybuddy.app to javafx.fxml;
}