package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuthController  {

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
    private Label oppureLabel;

    @FXML
    private void initialize() {
        if (authService.noUsers()) {
            loginBtn.setVisible(false);
            loginBtn.setManaged(false);
            oppureLabel.setVisible(false);
        }
    }


    @FXML
    void handleLoginBtn(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            mostraDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
            return;
        }
        boolean success = authService.login(username, password);
        if (success) {
            ViewLoader.load("menu");
        } else {
            mostraDialog(Alert.AlertType.ERROR, "Errore", "Credenziali non valide: username o password sbagliate.");
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
        if (username.length() > 15) {
            mostraDialog(Alert.AlertType.ERROR, "Username non valido", "L'username non può superare i 15 caratteri.");
            return;
        }
        if (password.length() < 6) {
            mostraDialog(Alert.AlertType.ERROR, "Password non valida", "La password deve contenere almeno 6 caratteri.");
            return;
        }
        boolean firstUser = authService.noUsers();
        boolean success = authService.register(username, password, firstUser);
        if (success) {
            String ruolo = firstUser ? "amministratore" : "utente";
            mostraDialog(Alert.AlertType.INFORMATION, "Registrazione completata", "Registrazione come " + ruolo +" effettuata con successo. \nContinua con il log in.");
            registerBtn.setVisible(false);
            registerBtn.setManaged(false);
            loginBtn.setVisible(true);
            loginBtn.setManaged(true);
        } else {
            mostraDialog(Alert.AlertType.ERROR, "Errore", "Utente già esistente.");
        }


    }

    public void mostraDialog(Alert.AlertType type, String titolo, String messaggio) {
        Alert alert = new Alert(type);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/it/unisa/diem/wordageddon_g16/style/dialog.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        switch (type) {
            case ERROR -> {
                dialogPane.getStyleClass().add("alert-error");
            }
            case INFORMATION -> {
                dialogPane.getStyleClass().add("alert-info");
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/it/unisa/diem/wordageddon_g16/asserts/confirm-icon.png")));
                icon.setFitHeight(40);
                icon.setFitWidth(40);
                dialogPane.setGraphic(icon);
            }
            default -> dialogPane.getStyleClass().add("alert-default");
        }
        alert.showAndWait();
    }

    private DAO<?> userDAO;

    public <T> AuthController(AuthService authService) {
        this.authService = authService;
    }



    // aggiungi checker per username e password

    // no utenti? solo registrazione e mettilo admin
}
