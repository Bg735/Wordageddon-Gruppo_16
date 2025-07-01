package it.unisa.diem.wordageddon_g16;

import it.unisa.diem.wordageddon_g16.controllers.*;
import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.scene.image.Image;

import java.io.IOException;

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

        Scene scene = new Scene(new StackPane(), 1280, 832);
        stage.setScene(scene);
        stage.setResizable(true);
        ViewLoader.setStage(stage);
        ViewLoader.setScene(scene);
        ViewLoader.setControllerFactory(controllerFactory);

        ViewLoader.load("authentication");
        /*if (context.getAuthService().restoreSession()) {
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