package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class AuthController {
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


    public <T> AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
}
