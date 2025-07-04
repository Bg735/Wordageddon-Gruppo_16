package it.unisa.diem.wordageddon_g16;

import it.unisa.diem.wordageddon_g16.controllers.*;
import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
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

        Callback<Class<?>,Object> controllerFactory = clazz -> switch (clazz.getSimpleName()) {
            case "AuthController" -> new AuthController(context);
            case "MainMenuController" -> new MainMenuController(context);
            case "GameSessionController" -> new GameSessionController();
            case "LeaderboardController" -> new LeaderboardController(context);
            case "UserPanelController" -> new UserPanelController(
                    new UserPanelService(
                            repo.getDAO("gameReport"),
                            repo.getDAO("user"),
                            repo.getDAO("document"),
                            repo.getDAO("stopWord"),
                            context
                    )
            );

            default -> {
                throw new RuntimeException("Failed to create controller");
            }
        };

        stage.setResizable(true);
        stage.setWidth(1280);
        stage.setHeight(832);
        stage.getIcons().add(new Image(Resources.getAsset("logo2.png")));
        stage.setScene(new Scene(new StackPane(), 1280, 832));
        stage.setMaximized(true);

        ViewLoader.setStage(stage);
        ViewLoader.setControllerFactory(controllerFactory);
        ViewLoader.load(ViewLoader.View.AUTH);


        if (context.getAuthService().loadSession()) {
            ViewLoader.load(ViewLoader.View.MENU);
        } else {
            ViewLoader.load(ViewLoader.View.AUTH);
        }

        System.out.println(Font.loadFont(getClass().getResourceAsStream("/it/unisa/diem/wordageddon_g16/fonts/Alata-Regular.ttf"), 12));
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