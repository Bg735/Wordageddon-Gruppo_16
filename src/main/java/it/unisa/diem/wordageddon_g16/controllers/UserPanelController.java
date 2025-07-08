package it.unisa.diem.wordageddon_g16.controllers;


import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @class UserPanelController
 * @brief Controller della schermata dedicata all' utente.
 *
 * Gestisce la logica della vista associata al pannello utente, inclusa la visualizzazione delle statistiche
 * personali,la gestione dei documenti, delle stopwords e degli utenti con privilegi amministrativi.
 *
 */

public class UserPanelController {
    private final UserPanelService service;

    @FXML
    private Pane adminPanel;

    @FXML
    private TableColumn<GameReport, String> livelloClm;

    @FXML
    private Label totalGameLabel;

    @FXML
    private Label avgScoreLabel;

    @FXML
    private Label maxScoreLabel;

    @FXML
    private TableColumn<GameReport, Integer> punteggioClm;

    @FXML
    private TableColumn<GameReport, String> tempoClm;

    @FXML
    private TableView<GameReport> userTableView;

    @FXML
    private Label usernameLabel;

    @FXML private AnchorPane anchorSemicerchio;

    @FXML private StackPane stackMedio;

    private final User currentUser;
    private final AppContext appContext;

    /**
     * Costruttore del controller.
     *
     * @param context Il contesto applicativo che fornisce accesso all'utente corrente e ai service delle interfacce.
     */
    public UserPanelController(AppContext context) {
        this.service = context.getUserPanelService();
        currentUser=context.getCurrentUser();
        this.appContext = context;
    }

    @FXML
    void handleAdmin(ActionEvent event) {
        PopupBuilder popup = new PopupBuilder("Gestione Ruoli Utenti", 450, 350);
        VBox root = popup.getRoot();
        List<User> otherUsers = service.getAllUsersExceptCurrent();

        if (otherUsers.isEmpty()) {
            Label noUsersLabel = new Label("Nessun altro utente disponibile.");
            noUsersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            root.getChildren().add(noUsersLabel);
        } else {
            for (User user : otherUsers) {
                VBox userBox = new VBox(5);
                HBox userRow = new HBox(20);
                userRow.setAlignment(Pos.CENTER_LEFT);
                userRow.setPadding(new Insets(5, 10, 5, 10));
                userRow.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 5;");

                Label nameLabel = new Label(user.getName());
                nameLabel.setStyle("-fx-text-fill: black;");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                ToggleButton toggle = new ToggleButton();
                toggle.setText(user.isAdmin() ? "Admin" : "User");
                toggle.setSelected(user.isAdmin());
                toggle.setStyle("-fx-font-size: 12px;");

                Label feedbackLabel = new Label();
                feedbackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
                feedbackLabel.setVisible(false); // Inizialmente nascosto

                toggle.setOnAction(e -> {
                    boolean nowAdmin = toggle.isSelected();
                    toggle.setText(nowAdmin ? "Admin" : "User");

                    if (nowAdmin) {
                        service.promoteUser(user.getName());
                    } else {
                        service.demoteUser(user.getName());
                    }

                    // Mostra il messaggio nel popup
                    feedbackLabel.setText("Ruolo aggiornato a " + (nowAdmin ? "Admin" : "User"));
                    feedbackLabel.setVisible(true);

                    PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1.5));
                    pause.setOnFinished(ev -> feedbackLabel.setVisible(false));
                    pause.play();
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                userRow.getChildren().addAll(nameLabel, spacer, toggle);
                userBox.getChildren().addAll(userRow, feedbackLabel);
                root.getChildren().add(userBox);
            }
        }
        popup.show();
    }

    /**
     * Gestisce il popup per la gestione dei documenti.
     *
     * Mostra un elenco dei documenti esistenti con possibilità di rimozione.
     * Consente anche il caricamento di un nuovo file `.txt`. Il contenuto viene sincronizzato con il database.
     *
     * @param event L'evento ActionEvent generato dal click sul menu.
     */
    @FXML
    void handleDocumenti(ActionEvent event) {
        PopupBuilder popup = new PopupBuilder("Gestione Documenti", 400, 300);
        VBox root = popup.getRoot();

        ObservableList<Document> documentList = FXCollections.observableArrayList(service.getAllDocuments());

        ListView<Document> listView = new ListView<>(documentList);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox content = new HBox(10);
            private final Label label = new Label();
            private final Button removeBtn = new Button("Rimuovi");
            {
                label.setTextFill(Color.BLACK);
                HBox.setHgrow(label, Priority.ALWAYS);
                content.setAlignment(Pos.CENTER);
                content.setPadding(new Insets(2, 1, 2, 1));
                label.setAlignment(Pos.BASELINE_LEFT);
                content.getChildren().addAll(label, removeBtn);
            }

            @Override
            protected void updateItem(Document doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) {
                    setGraphic(null);
                } else {
                    label.setText(doc.title());
                    label.setStyle("-fx-text-fill:black");
                    removeBtn.setOnAction(e -> {
                        service.deleteDocument(doc);
                        documentList.remove(doc);
                    });
                    setGraphic(content);
                }
            }
        });

        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size: 11px; ");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);

        PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1.5));
        pause.setOnFinished(e -> feedbackLabel.setVisible(false));

        Button uploadBtn = new Button("Carica nuovo documento (.txt)");
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona un file .txt");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt")
            );

            File file = fileChooser.showOpenDialog(popup.getStage());
            if (file != null) {
                try {
                    Document newDoc = service.addDocument(file);
                    if (newDoc != null) {
                        documentList.setAll(service.getAllDocuments());
                        feedbackLabel.setText("Documento caricato con successo!");
                        feedbackLabel.setStyle("-fx-text-fill: white;");
                    } else {
                        feedbackLabel.setText("Il documento è già presente.");
                    }
                } catch (Exception ex) {
                    SystemLogger.log("Errore durante il caricamento di un documento", ex);
                    feedbackLabel.setText("Errore durante il caricamento.");
                }

                feedbackLabel.setVisible(true);
                pause.playFromStart();
            }
        });


        root.getChildren().addAll(
                new Label("Documenti esistenti:"),
                listView,
                uploadBtn,
                feedbackLabel
        );
        popup.show();
    }

    /**
     * Effettua il logout dell'utente corrente e ritorna alla schermata di autenticazione.
     *
     * @param event L'evento ActionEvent generato dal click sul pulsante "Logout".
     */
    @FXML
     void handleLogOut(ActionEvent event) {
        appContext.getAuthService().logout();
        ViewLoader.load(ViewLoader.View.AUTH);
    }

    /**
     * Gestisce il ritorno alla schermata del menu principale.
     *
     * @param event L'evento ActionEvent generato dal click sul pulsante "Indietro".
     */
    @FXML
    void handleGoBack(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.MENU);
    }

    /**
     * Gestisce il popup per la gestione delle stopwords.
     *
     * Permette l'aggiunta manuale tramite TextInput, il caricamento da file `.txt` e la rimozione di stopwords.
     * Il contenuto viene sincronizzato con il database.
     *
     * @param event L'evento ActionEvent generato dal click sul menu.
     */
    @FXML
    void handleStopWords(ActionEvent event) {
        PopupBuilder popup = new PopupBuilder("Gestione Documenti", 400, 500);
        VBox root = popup.getRoot();

        //permetto di aggiungere una stopword
        TextField wordInput = new TextField();
        wordInput.setPromptText("Inserisci una nuova stopword");

        //le inserisco in una lista di sw
        ListView<String> sw = new ListView<>();
        sw.getItems().addAll(service.getAllStopwords());

        //aggiunta manuale sw
        Button btnAdd = new Button("Aggiungi");
        btnAdd.setOnAction(e -> {
            String word = wordInput.getText().trim().toLowerCase();
            if (!word.isEmpty() && !sw.getItems().contains(word)) {
                try {
                    service.addStopWords(word);
                    sw.getItems().add(word);
                    wordInput.clear();
                } catch (Exception ex) {
                    SystemLogger.log("Errore durante l'aggiunta di una stopword", ex);
                }
            }
        });

        //carico file
        Button btnFile = new Button("Carica da file");
        btnFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona file di stopwords");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(popup.getStage());
            if (file != null) {
                try {
                    service.addStopwordsFromFile(file);
                    sw.getItems().setAll(service.getAllStopwords());
                } catch (RuntimeException ex) {
                    SystemLogger.log("Errore di stopwords", ex);
                    ex.printStackTrace();
                } catch (IOException ex) {
                    SystemLogger.log("Errore nella chiusura del file di testo", ex);
                    throw new RuntimeException(ex);
                }
            }
        });
        Button removeButton = new Button("Rimuovi selezionata");
        removeButton.setOnAction(e -> {
            String selected = sw.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try{
                service.deleteStopword(selected);
                sw.getItems().remove(selected);
            } catch (Exception ex) {
                    SystemLogger.log("Errore durante la rimozione di una stopword", ex);
                }
                }
        });
        root.getChildren().addAll(
                new Label("Inserisci una nuova stopword:"),
                wordInput,
                btnAdd,
                new Label("Oppure caricala da file:"),
                btnFile,
                new Label("StopWords attuali:"),
                sw, removeButton );
        root.setAlignment(Pos.CENTER);
        popup.show();
    }


    /**
     * Metodo di inizializzazione automatico chiamato da JavaFX.
     *
     * Imposta le informazioni iniziali dell'interfaccia: etichetta dell'utente, visualizzazione menu per gli admin,
     * tabella dei report di gioco e statistiche, e comportamenti grafici dinamici.
     */
    @FXML
    public void initialize() {
        // Posiziona il secondo StackPane a metà altezza dell'AnchorPane a forma di semicerchio
        anchorSemicerchio.heightProperty().addListener((obs, oldVal, newVal) -> {
            double centerY = (newVal.doubleValue() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });
        Platform.runLater(() -> {
            double centerY = (anchorSemicerchio.getHeight() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });


        var isAdmin= currentUser.isAdmin();
        if(!isAdmin){
            adminPanel.setVisible(false);
        }

        usernameLabel.setText(currentUser.getName());

        livelloClm.setCellValueFactory(report -> new SimpleStringProperty(report.getValue().getDifficulty().toString()));
        punteggioClm.setCellValueFactory(new PropertyValueFactory<>("score"));
        tempoClm.setCellValueFactory(report -> {
            Duration dur = report.getValue().getUsedTime();
            String formatted = String.format("%02d:%02d", dur.toMinutesPart(), dur.toSecondsPart());
            return new SimpleStringProperty(formatted);
        });

        List<GameReport> reports = service.getCurrentUserReports();
        userTableView.getItems().setAll(reports);


        Map<String, Object> stats = service.getUserStatsForCurrentUser();
        totalGameLabel.setText(String.valueOf(stats.get("totalGames")));
        avgScoreLabel.setText(String.format("%.1f", stats.get("averageScore")));
        maxScoreLabel.setText(String.valueOf(stats.get("maxScore")));
    }

    /**
     @class PopupBuilder

     @brief Classe di utilità per creare popup modali JavaFX con layout VBox.

     Facilita la costruzione e visualizzazione di popup riutilizzabili con stile uniforme.
     */
    private class PopupBuilder {
        private final Stage stage;
        private final VBox root;

        public PopupBuilder(String title, int width, int height) {
            this.stage = new Stage();
            this.root = new VBox(15);
            this.root.setPadding(new Insets(15));
            this.root.setAlignment(Pos.CENTER);
            this.stage.setTitle(title);
            this.stage.setHeight(height);
            this.stage.setWidth(width);
            this.stage.initModality(Modality.APPLICATION_MODAL);
            this.stage.setResizable(false);
        }

        public VBox getRoot() {
            return root;
        }

        public void show() {
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Resources.getStyle("popup"));
            stage.setScene(scene);
            stage.showAndWait();
        }

        public Stage getStage() {
            return stage;
        }
    }

}