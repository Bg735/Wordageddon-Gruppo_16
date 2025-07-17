package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameSessionState;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller della schermata principale del menu dell'applicazione Wordageddon.
 * Gestisce l'inizializzazione della vista, il caricamento dell'utente corrente,
 * il recupero di sessioni di gioco precedenti e la navigazione verso altre viste.
 */
public class MainMenuController implements Initializable {

    /**
     * Label per visualizzare il nome dell'utente attualmente loggato.
     */
    @FXML
    private Label usernameLabel;

    /**
     * Contesto applicativo condiviso contenente informazioni sulla sessione.
     */
    private final AppContext context;

    /**
     * Costruttore che inizializza il controller con il contesto applicativo.
     *
     * @param context Il contesto applicativo corrente.
     */
    public MainMenuController(AppContext context) {
        this.context=context;
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX.
     * Imposta il nome dell'utente loggato nella label e verifica la presenza
     * di una sessione di gioco salvata.
     *
     * @param url URL di inizializzazione (non utilizzato).
     * @param resourceBundle Risorse internazionalizzate (non utilizzato).
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User user = context.getCurrentUser();
        usernameLabel.setText(user.getName());

        File interruptedSessionFile = new File("interruptedSession.ser");
        if (interruptedSessionFile.exists()) {
            try (var in = new ObjectInputStream(new FileInputStream("interruptedSession.ser"))) {
                System.out.println("File di sessione interrotta trovato: " + interruptedSessionFile.getName());
                var gameSessionState = (GameSessionState) in.readObject();
                User foundUser = gameSessionState.getUser();
                if (foundUser.equals(user)) {
                    System.out.println("Rilevata sessione interrotta per l'utente: " + foundUser.getName());

                    // Mostra Alert di conferma nella JavaFX Application Thread
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Riprendi partita interrotta");
                        alert.setHeaderText("Partita interrotta rilevata");
                        alert.setContentText("Vuoi continuare la partita interrotta?");
                        ButtonType buttonYes = new ButtonType("Sì", ButtonBar.ButtonData.YES);
                        ButtonType buttonNo = new ButtonType("No", ButtonBar.ButtonData.NO);
                        alert.getButtonTypes().setAll(buttonYes, buttonNo);
                        alert.initOwner(usernameLabel.getScene().getWindow());

                        alert.showAndWait().ifPresent(choice -> {
                            if (choice == buttonYes) {
                                context.setInterruptedSession(gameSessionState);
                                ViewLoader.load(ViewLoader.View.GAME);
                            } else {
                                // Se l’utente rifiuta, elimina il file di sessione interrotta
                                interruptedSessionFile.delete();
                            }
                        });
                    });
                } else {
                    // Sessione di un altro utente: elimina il file
                    interruptedSessionFile.delete();
                }
            } catch (IOException | ClassNotFoundException e) {
                SystemLogger.log("Errore durante la deserializzazione del file " + interruptedSessionFile.getName(), e);
            }
        }
    }


    /**
     * Gestisce la richiesta di visualizzazione della classifica da parte dell'utente.
     * Carica la vista LEADERBOARD tramite il ViewLoader.
     */
    @FXML
    private void onLeaderboardRequested() {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }

    /**
     * Gestisce la richiesta di visualizzazione del pannello utente da parte dell'utente.
     * Carica la vista USER_PANEL tramite il ViewLoader.
     */
    @FXML
    private void onUserPanelRequested() {
        ViewLoader.load(ViewLoader.View.USER_PANEL);
    }

    /**
     * Gestisce l'avvio della partita.
     * Verifica se sono presenti documenti disponibili; in caso negativo, mostra un messaggio di errore.
     * In caso contrario, carica la vista del gioco.
     */
    @FXML
    private void playGame() {
        if(!(context.getRepo().<Document,DocumentDAO>getDAO("document")).selectAll().isEmpty())
            ViewLoader.load(ViewLoader.View.GAME);
        else{
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Non sono presenti documenti per il gioco. Un amministratore deve caricare dei documenti per poter giocare.");
            alert.getDialogPane().getStyleClass().add(Resources.getStyle("dialog"));
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(Resources.getStyle("dialog"));
            dialogPane.getStyleClass().add("alert-error");
            alert.show();
        }
    }
}
