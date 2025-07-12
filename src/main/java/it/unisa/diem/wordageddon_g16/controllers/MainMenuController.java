package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.utility.Popup;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;



public class MainMenuController implements Initializable {

    @FXML
    private Button leaderBoardBtn;

    @FXML
    private Button startBtn;

    @FXML
    private AnchorPane userPanelBtn;

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
    }

    @FXML
    void onLeaderboardRequested(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }

    @FXML
    void onUserPanelRequested(MouseEvent event) {
        ViewLoader.load(ViewLoader.View.USER_PANEL);
    }

    @FXML
    void playGame(ActionEvent event) {
        if(!(context.getRepo().<Document,DocumentDAO>getDAO("document")).selectAll().isEmpty())
            ViewLoader.load(ViewLoader.View.GAME);
        else{
            Alert alert=new Alert(Alert.AlertType.ERROR, "Non sono presenti documenti da utilizzare per il gioco. Un amministratore deve caricare dei documenti per poter giocare.", ButtonType.CLOSE);
            alert.getDialogPane().getStyleClass().add(Resources.getStyle("alert"));
            alert.show();
        }
    }
}