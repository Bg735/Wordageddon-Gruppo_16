package it.unisa.diem.wordageddon_g16;

import it.unisa.diem.wordageddon_g16.controllers.*;
import it.unisa.diem.wordageddon_g16.db.JdbcRepository;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.image.Image;

import java.io.*;

/**
 * Classe principale dell'applicazione Wordageddon.
 * <p>
 * Avvia l'interfaccia JavaFX, inizializza il {@link AppContext}, registra i controller necessari tramite
 * {@link javafx.util.Callback} e carica la vista iniziale tramite {@link ViewLoader}.
 * <br>
 * La finestra viene configurata con dimensioni iniziali, icona personalizzata e gestione della chiusura.
 */
public class WordageddonApp extends Application {

    /**
     * Punto di ingresso principale dell'applicazione JavaFX.
     * <p>
     * Inizializza le dipendenze e il repository, costruisce il {@link AppContext} condiviso e imposta
     * la factory dei controller per la navigazione tra viste.
     * <br>
     * Configura la finestra ({@link Stage}) con dimensioni, icona, e comportamento al termine.
     * Se esiste una sessione utente attiva, apre il menu; altrimenti la schermata di autenticazione.
     * </p>
     *
     * @param stage finestra primaria dell'applicazione
     */
    @Override
    public void start(Stage stage){
        var repo = new JdbcRepository();
        var context = new AppContext(repo);

        Callback<Class<?>,Object> controllerFactory = clazz -> switch (clazz.getSimpleName()) {
            case "AuthController" -> new AuthController(context);
            case "MainMenuController" -> new MainMenuController(context);
            case "GameController" -> new GameController(context);
            case "LeaderboardController" -> new LeaderboardController(context);
            case "UserPanelController" -> new UserPanelController(context);

            default -> throw new RuntimeException("Failed to create controller");
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

        stage.setOnCloseRequest(_ -> {
            repo.close();
            if (ViewLoader.getCurrentView().equals(ViewLoader.View.GAME)){
                var controller = (GameController) ViewLoader.getCurrentController();
                try(var out = new ObjectOutputStream(new FileOutputStream("session.ser"))) {
                    out.writeObject(controller.getGameService());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        stage.setTitle("Wordageddon");
        stage.show();
    }

    /**
     * Metodo main dell'applicazione.
     * <p>
     * Lancia l'applicazione JavaFX.
     * </p>
     *
     * @param args argomenti da linea di comando (non utilizzati)
     */
    public static void main(String[] args) {
        launch();
    }
}