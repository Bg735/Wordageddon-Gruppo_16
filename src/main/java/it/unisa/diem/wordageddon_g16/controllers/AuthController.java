package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.services.Config;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

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

    private boolean noUsers;

    @FXML
    private void initialize() {
        noUsers = authService.noUsers();
        if (noUsers) {
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
            showDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
            return;
        }
        boolean success = authService.login(username, password);
        if (success) {
            ViewLoader.load("menu");
        } else {
            showDialog(Alert.AlertType.ERROR, "Errore", "Credenziali non valide: username o password sbagliate.");
        }

    }

    @FXML
    void handleRegisterBtn(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        int maxUsernameLength = Integer.parseInt(Config.get(Config.Props.USR_CHAR_MAX_LENGTH));
        int minPasswordLength = Integer.parseInt(Config.get(Config.Props.PW_CHAR_MIN_LENGTH));

        if (username.isEmpty() || password.isEmpty()) {
            showDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
            return;
        }
        if (username.length() > maxUsernameLength) {
            showDialog(Alert.AlertType.ERROR, "Username non valido", "L'username non può superare i "+ maxUsernameLength +"caratteri.");
            return;
        }
        if (password.length() < minPasswordLength) {
            showDialog(Alert.AlertType.ERROR, "Password non valida", "La password deve contenere almeno "+ minPasswordLength +" caratteri.");
            return;
        }

        if (authService.register(username, password, noUsers)) {
            String ruolo = noUsers ? "amministratore" : "utente";
            showDialog(Alert.AlertType.INFORMATION, "Registrazione completata", "Registrazione come " + ruolo +" effettuata con successo. \nContinua con il log in.");
            registerBtn.setVisible(false);
            registerBtn.setManaged(false);
            loginBtn.setVisible(true);
            loginBtn.setManaged(true);
        } else {
            showDialog(Alert.AlertType.ERROR, "Errore", "L'utente inserito esiste già.");
        }


    }

    public void showDialog(Alert.AlertType type, String titolo, String messaggio) {
        Alert alert = new Alert(type);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(Resources.getStyle("dialog.css"));
        dialogPane.getStyleClass().add("dialog-pane");
        switch (type) {
            case ERROR -> {
                dialogPane.getStyleClass().add("alert-error");
            }
            case INFORMATION -> {
                dialogPane.getStyleClass().add("alert-info");
                ImageView icon = new ImageView(new Image(Resources.getAsset("confirm-icon.png")));
                icon.setFitHeight(40);
                icon.setFitWidth(40);
                dialogPane.setGraphic(icon);
            }
            default -> dialogPane.getStyleClass().add("alert-default");
        }
        alert.showAndWait();
    }

    public <T> AuthController(AuthService authService) {
        this.authService = authService;
    }

}
