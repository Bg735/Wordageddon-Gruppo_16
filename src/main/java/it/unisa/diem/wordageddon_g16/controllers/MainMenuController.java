package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class MainMenuController implements Initializable {

    @FXML
    private Button leaderBoardBtn;

    @FXML
    private Button startBtn;

    @FXML
    private Label usernameLabel;
    private final AppContext context;

    @FXML
    void handleLeaderBoardBtn(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }

    @FXML
    void handleOpenUserPanel(MouseEvent event) {
        ViewLoader.load(ViewLoader.View.USER_PANEL);
    }

    @FXML
    void handleStartBtn(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.GAME);
    }

    public MainMenuController(AppContext context) {
        this.context=context;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User  user= context.getCurrentUser();
        usernameLabel.setText(user.getName());
    }
}
