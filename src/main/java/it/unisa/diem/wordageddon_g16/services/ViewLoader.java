package it.unisa.diem.wordageddon_g16.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    private static Scene scene;

    public static void setScene(Scene s) {
        scene = s;
    }
    public static void load(String fxmlView){
        if (controllerFactory == null || stage == null || scene == null) {
            throw new IllegalStateException("ViewLoader not properly initialized.");
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ViewLoader.class.getResource("/it/unisa/diem/wordageddon_g16/fxml/" + fxmlView + ".fxml"));
            fxmlLoader.setControllerFactory(controllerFactory);
            Parent root = fxmlLoader.load();
            scene.setRoot(root);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML view: " + fxmlView, e);
        }
    }
}
