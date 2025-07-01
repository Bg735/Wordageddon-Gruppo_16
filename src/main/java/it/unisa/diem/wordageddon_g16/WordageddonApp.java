package it.unisa.diem.wordageddon_g16;

import it.unisa.diem.wordageddon_g16.controllers.*;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.Objects;

public class WordageddonApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {;

        var repo = new JdbcRepository();

        var context = new AppContext(repo);


        Callback<Class<?>, Object> controllerFactory = clazz -> switch (clazz.getSimpleName()) {
            case "AuthController" -> new AuthController(context.getAuthService());
            case "MainMenuController" -> new MainMenuController(context);
            //case "GameSessionController" -> new GameSessionController(repo.<Document,Long>getDAO("document"), repo.<String,Object>getDAO("stopword"), repo.<WDM,Long>getDAO("wdm"));
           // case "LeaderboardController" -> new LeaderboardController(repo.getDAO("gameReport"));
          //  case "UserPanelController" -> new UserPanelController(repo.getDAO("user"), repo.getDAO("gameReport"));

            default -> {
                throw new RuntimeException("Failed to create controller");
            }
        };

        stage.setResizable(true);
        stage.setWidth(1280);
        stage.setHeight(832);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/it/unisa/diem/wordageddon_g16/assets/logo2.png"))));

        ViewLoader.setStage(stage);
        ViewLoader.setControllerFactory(controllerFactory);
        ViewLoader.load("authentication");

        /*
        if (context.getAuthService().restoreSession()) {
            ViewLoader.load("menu");
        } else {
            ViewLoader.load("authentication");
        }
*/
        stage.setOnCloseRequest(event -> {
            repo.close();
        });
        stage.setTitle("Wordageddon");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}