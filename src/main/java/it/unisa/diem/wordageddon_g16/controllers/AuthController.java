package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.utility.Config;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller JavaFX per la schermata di autenticazione dell'applicazione Wordageddon.
 * <p>
 * Gestisce il login e la registrazione degli utenti, la visualizzazione dinamica della password,
 * e la validazione dei campi tramite {@link AuthService}. Utilizza {@link ViewLoader} per la navigazione
 * e {@link Resources} per applicare lo stile ai popup di dialogo.
 */
public class AuthController {

    private final AuthService authService;
    @FXML
    private Button loginBtn;

    @FXML
    private Button registerBtn;

    @FXML
    private PasswordField passwordPF;

    @FXML
    private TextField passwordTF;

    @FXML
    private TextField usernameField;

    @FXML
    private Label oppureLabel;

    @FXML
    private CheckBox showPasswordCB;

    private boolean noUsers;

    /**
     * Inizializza la schermata di autenticazione.
     * <p>
     * Mostra il pulsante di registrazione se non ci sono utenti, e nasconde il login.
     * Configura anche il binding tra {@link PasswordField} e {@link TextField} per la visualizzazione della password.
     */
    @FXML
    private void initialize() {
        noUsers = authService.noUsers();
        if (noUsers) {
            loginBtn.setVisible(false);
            loginBtn.setManaged(false);
            registerBtn.setDefaultButton(true);
            oppureLabel.setVisible(false);
        }
        else{
            loginBtn.setDefaultButton(true);
        }

        passwordPF.visibleProperty().bind(showPasswordCB.selectedProperty().not());
        passwordPF.managedProperty().bind(showPasswordCB.selectedProperty().not());
        passwordTF.visibleProperty().bind(showPasswordCB.selectedProperty());
        passwordTF.managedProperty().bind(showPasswordCB.selectedProperty());
        passwordTF.textProperty().bindBidirectional(passwordPF.textProperty());
    }

    /**
     * Gestisce il clic sul pulsante di login.
     * <p>
     * Verifica che i campi siano compilati, tenta l'autenticazione e reindirizza al menu se riuscita.
     * Se fallisce, mostra un messaggio di errore tramite {@link #showDialog(Alert.AlertType, String, String)}.
     *
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordPF.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showDialog(Alert.AlertType.ERROR, "Campi incompleti", "Inserisci username e password.");
            return;
        }
        if (authService.login(username, password)) {
            ViewLoader.load(ViewLoader.View.MENU);
        } else {
            showDialog(Alert.AlertType.ERROR, "Errore", "Credenziali non valide: username o password sbagliate.");
        }

    }

    /**
     * Gestisce la registrazione di un nuovo utente.
     * <p>
     * Valida l'input rispetto ai vincoli definiti in {@link Config}, registra l'utente e carica il menu se riuscito.
     * Altrimenti, mostra un messaggio di errore.
     *
     */
    @FXML
    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = passwordPF.getText().trim();
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
            ViewLoader.load(ViewLoader.View.MENU);
        } else {
            showDialog(Alert.AlertType.ERROR, "Errore", "L'utente inserito esiste già.");
        }


    }

    /**
     * Mostra una finestra di dialogo personalizzata con stile dinamico basato sul tipo di {@link Alert}.
     * <p>
     * Se il tipo è {@code INFORMATION}, mostra un'icona di conferma.
     *
     * @param type      tipo di alert da visualizzare
     * @param titolo    titolo della finestra
     * @param messaggio contenuto testuale del messaggio
     */
    public void showDialog(Alert.AlertType type, String titolo, String messaggio) {
        Alert alert = new Alert(type);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(Resources.getStyle("dialog"));
        dialogPane.getStyleClass().add("dialog-pane");
        switch (type) {
            case ERROR -> dialogPane.getStyleClass().add("alert-error");
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

    /**
     * Costruisce il controller associando il servizio di autenticazione tramite {@link AppContext}.
     *
     * @param context contesto applicativo contenente l'istanza di {@link AuthService}
     */
    public AuthController(AppContext context) {
        this.authService = context.getAuthService();
    }

}
