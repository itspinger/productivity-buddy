module org.productivitybuddy.productivitybuddy {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires com.google.gson;
    requires jakarta.annotation;
    requires com.github.oshi;

    requires org.slf4j;

    opens org.productivitybuddy;
    exports org.productivitybuddy;

    opens org.productivitybuddy.app;
    exports org.productivitybuddy.app;

    opens org.productivitybuddy.config;
    opens org.productivitybuddy.model;
    opens org.productivitybuddy.registry;
    opens org.productivitybuddy.registry.impl;
    opens org.productivitybuddy.lifecycle;
    opens org.productivitybuddy.store;
    opens org.productivitybuddy.store.impl;
    opens org.productivitybuddy.service;
    opens org.productivitybuddy.service.impl;
    opens org.productivitybuddy.controller;
    opens org.productivitybuddy.ui;
    opens org.productivitybuddy.view;
    opens org.productivitybuddy.thread;
    opens org.productivitybuddy.util;

    exports org.productivitybuddy.controller;
}
