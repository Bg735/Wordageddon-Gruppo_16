package it.unisa.diem.wordageddon_g16.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

public class ViewLoader {
    private static Callback<Class<?>, Object> controllerFactory;
    private static Stage stage;

    public static void setControllerFactory(Callback<Class<?>, Object> factory) {
        controllerFactory = factory;
    }

    public static void setStage(Stage stage) {
        ViewLoader.stage = stage;
    }

    public static void load(String fxmlView){
        if (controllerFactory == null || stage == null) {
            throw new IllegalStateException("ViewLoader is not properly set. Please set the controllerFactory and stage before loading views.");
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ViewLoader.class.getResource(fxmlView+".fxml"));
            fxmlLoader.setControllerFactory(controllerFactory);
            stage.setScene(new Scene(fxmlLoader.load(), 1920, 1080));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML view: " + fxmlView, e);
        }
    }
}
