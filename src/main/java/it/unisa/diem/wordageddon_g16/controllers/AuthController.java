package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class AuthController {

    private final AuthService authService;
    @FXML
    private Button loginBtn;

    @FXML
    private Button registerBtn;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField usernameField;

    @FXML
    void handleLoginBtn(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            mostraDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
        }
        boolean success = authService.login(username, password);
        if (success) {
            ViewLoader.load("menu");
        } else {
            mostraDialog(Alert.AlertType.ERROR, "Errore", "Credenziali non valide.");
        }

    }

    @FXML
    void handleRegisterBtn(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            mostraDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
            return;
        }

        boolean firstUser = authService.noUsers();
        boolean success = authService.register(username, password, firstUser);
        if (success) {
            String ruolo = firstUser ? "amministratore" : "utente";
            mostraDialog(Alert.AlertType.INFORMATION, "Registrazione completata", "Registrato come " + ruolo );
            // TODO: Naviga alla schermata principale
        } else {
            mostraDialog(Alert.AlertType.ERROR, "Errore", "Utente già esistente.");
        }


    }

    public void mostraDialog(Alert.AlertType type, String titolo, String messaggio) {
        Alert alert = new Alert(type);
        alert.setTitle(titolo);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private DAO<?> userDAO;

    public <T> AuthController(AuthService authService) {
        this.authService = authService;
    }



    // aggiungi checker per username e password

    // no utenti? solo registrazione e mettilo admin
}
