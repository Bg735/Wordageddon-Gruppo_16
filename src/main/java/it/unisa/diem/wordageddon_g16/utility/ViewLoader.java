package it.unisa.diem.wordageddon_g16.utility;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

public class ViewLoader {
    private static Callback<Class<?>, Object> controllerFactory;
    private static Stage stage;

    public enum View{
        AUTH("authentication"),
        MENU("menu"),
        GAME("game"),
        USER_PANEL("userPanel"),
        LEADERBOARD("leaderboard"),
        REPORT("report");

        private final String viewName;

        View(String viewName) {
            this.viewName = viewName;
        }

        public String get() {
            return viewName;
        }
    }

    public static void setControllerFactory(Callback<Class<?>, Object> factory) {
        controllerFactory = factory;
    }

    public static void setStage(Stage stage) {
        ViewLoader.stage = stage;
    }

    public static void load(View view){
        if (controllerFactory == null || stage == null) {
            throw new IllegalStateException("ViewLoader not properly initialized.");
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ViewLoader.class.getResource(Resources.RES_PATH+"fxml/" + view.get() + ".fxml"));
            fxmlLoader.setControllerFactory(controllerFactory);
            stage.getScene().setRoot(fxmlLoader.load());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML view: " + view.get(), e);
        }
    }
}