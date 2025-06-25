package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class AuthController {

    private final AuthService authService;
    @FXML
    private Button RegistrazioneBtn;

    @FXML
    private Button accediBtn;

    @FXML
    private TextField passwordTextField;

    @FXML
    private TextField userTextField;

    @FXML
    void handleAccediBtn(ActionEvent event) {

    }

    @FXML
    void handleRegistrazioneBtn(ActionEvent event) {

    }

    private DAO<?> userDAO;

    public <T> AuthController(AuthService authService) {
        this.authService = authService;
    }



    // aggiungi checker per username e password

    // no utenti? solo registrazione e mettilo admin
}
