package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.utility.Popup;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ResourceBundle;



public class MainMenuController implements Initializable {

    @FXML
    private Label usernameLabel;

    private final AppContext context;

    public MainMenuController(AppContext context) {
        this.context=context;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User  user= context.getCurrentUser();
        usernameLabel.setText(user.getName());
        if(new File("session.ser").exists()) {
            try (var in = new ObjectInputStream(new FileInputStream("session.ser"))) {
                if (in.available() > 0) {
                    var gameService = (GameService) in.readObject();
                    //caricare la partita salvata
                }
            }
            catch (IOException | ClassNotFoundException e) {
                    // No previous session found, continue with a new one
            }
        }

    }

    @FXML
    private void onLeaderboardRequested(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }

    @FXML
    private void onUserPanelRequested(MouseEvent event) {
        ViewLoader.load(ViewLoader.View.USER_PANEL);
    }

    @FXML
    private void playGame(ActionEvent event) {
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