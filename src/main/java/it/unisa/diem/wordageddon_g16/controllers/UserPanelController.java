package it.unisa.diem.wordageddon_g16.controllers;


import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.utility.Popup;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestisce la logica della vista associata al pannello utente, inclusa la visualizzazione delle statistiche
 * personali,la gestione dei documenti, delle stopwords e degli utenti con privilegi amministrativi.
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

    @FXML
    private AnchorPane anchorSemicerchio;

    @FXML
    private StackPane stackMedio;

    private final User currentUser;
    private final AppContext appContext;

    // Variabile per impedire il ricalcolo delle WDM se già in corso
    private final AtomicBoolean isRecalculatingWDMs;
    private final AtomicBoolean needsRecalculation;

    // Thread pool globale per il ricalcolo delle WDM
    ExecutorService threadPool;

    /**
     * Costruttore del controller.
     *
     * @param context Il contesto applicativo che fornisce accesso all'utente corrente e ai service delle interfacce.
     */
    public UserPanelController(AppContext context) {
        this.service = context.getUserPanelService();
        currentUser = context.getCurrentUser();
        this.appContext = context;
        isRecalculatingWDMs = new AtomicBoolean(false);
        needsRecalculation = new AtomicBoolean(false);
        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }

    @FXML
    private void handleAdmin() {
        Popup popup = new Popup("Gestione Ruoli Utenti");
        List<User> otherUsers = service.getAllUsersExceptCurrent();

        if (otherUsers.isEmpty()) {
            Label noUsersLabel = new Label("Nessun altro utente disponibile.");
            noUsersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            popup.addAll(noUsersLabel);
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

                toggle.setOnAction(_ -> {
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
                    pause.setOnFinished(_ -> feedbackLabel.setVisible(false));
                    pause.play();
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                userRow.getChildren().addAll(nameLabel, spacer, toggle);
                userBox.getChildren().addAll(userRow, feedbackLabel);
                popup.addAll(userBox);
            }
        }
        popup.show();
    }

    /**
     * Gestisce il popup per la gestione dei documenti.
     * <p>
     * Mostra un elenco dei documenti esistenti con possibilità di rimozione.
     * Consente anche il caricamento di un nuovo file `.txt`. Il contenuto viene sincronizzato con il database.
     */
    @FXML
    private void handleDocumenti() {
        Popup popup = new Popup("Gestione Documenti", 400, 300);
        ObservableList<Document> documentList = FXCollections.observableArrayList(service.getAllDocuments());
        ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

        ListView<Document> listView = new ListView<>(documentList);
        listView.setCellFactory(_ -> new ListCell<>() {
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
                    removeBtn.setOnAction(_ -> {
                        dbExecutor.execute(() -> service.deleteDocument(doc));
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
        pause.setOnFinished(_ -> feedbackLabel.setVisible(false));

        TextField titleTF = new TextField();

        titleTF.setPromptText("Titolo del documento");

        Button uploadBtn = new Button("Carica nuovo documento (.txt)");
        uploadBtn.setDefaultButton(true);
        uploadBtn.setGraphic(titleTF);
        uploadBtn.setOnAction(_ -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona un file .txt");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt")
            );
            File selectedFile = fileChooser.showOpenDialog(popup.getStage());
            if (selectedFile != null) {
                try {
                    service.addDocument(selectedFile);
                    Document tempDoc = new Document(selectedFile.getName(), (titleTF.getText().isEmpty() ? service.symbolicNameOf(selectedFile.getName()) : titleTF.getText()), null);
                    documentList.add(tempDoc);

                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            // Creo la WDM e la inserisco nel database
                            service.updateWDM(new WDM(tempDoc, service.getStopwords()));
                            return null;
                        }
                    };
                    task.setOnFailed(_ -> {
                        Throwable ex = task.getException();
                        Platform.runLater(() -> {
                            SystemLogger.log("[" + getClass().getName() + "]Task Execution Error:", ex);
                            feedbackLabel.setText(ex.getMessage());
                            feedbackLabel.setVisible(true);
                            pause.playFromStart();
                        });
                    });
                    dbExecutor.execute(task);
                } catch (FileAlreadyExistsException ex) {
                    feedbackLabel.setText("Il documento è già presente.");
                    feedbackLabel.setStyle("-fx-text-fill: yellow;");
                } catch (IOException ex) {
                    SystemLogger.log("Errore durante il caricamento di un documento", ex);
                    feedbackLabel.setText("Errore durante il caricamento.");
                    feedbackLabel.setStyle("-fx-text-fill: red;");
                } finally {
                    feedbackLabel.setVisible(true);
                    pause.playFromStart();
                }
                titleTF.clear();
            }
        });
        popup.addAll(
                new Label("Documenti esistenti:"),
                listView,
                uploadBtn,
                feedbackLabel
        );
        popup.show();
        dbExecutor.shutdown();
    }

    /**
     * Effettua il logout dell'utente corrente e ritorna alla schermata di autenticazione.
     */
    @FXML
    private void handleLogOut() {
        appContext.getAuthService().logout();
        ViewLoader.load(ViewLoader.View.AUTH);
    }

    /**
     * Gestisce il ritorno alla schermata del menu principale.
     */
    @FXML
    private void handleGoBack() {
        ViewLoader.load(ViewLoader.View.MENU);
    }

    /**
     * Gestisce il popup per la gestione delle stopwords.
     * <p>
     * Permette l'aggiunta manuale tramite TextInput, il caricamento da file `.txt` e la rimozione di stopwords.
     * Il contenuto viene sincronizzato con il database.
     */
    @FXML
    private void handleStopWords() {
        AtomicBoolean isSWChanged = new AtomicBoolean(false);
        Popup popup = new Popup("Gestione Stopwords", 400, 500);

        TextField tf = new TextField();
        tf.setPromptText("Inserisci stopwords");

        //le inserisco in una lista di sw
        ListView<String> sw = new ListView<>();
        sw.getItems().addAll(service.getStopwords());

        //aggiunta manuale sw
        Button btnAdd = new Button("Aggiungi");

        btnAdd.setOnAction(_ -> {
            isSWChanged.set(true);
            service.addStopWords(tf.getText());
            sw.getItems().setAll(service.getStopwords()); // Aggiorna la ListView senza duplicati
            tf.clear();
        });

        /*
         * Caricamento da file di stopwords
         */
        Button btnFile = new Button("Aggiungi da file");

        btnFile.setOnAction(_ -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona file di stopwords");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(popup.getStage());
            if (file == null) return;
            try {
                service.addStopwordsFromFile(file);
                sw.getItems().setAll(service.getStopwords());
                isSWChanged.set(true);
            } catch (IOException e) {
                SystemLogger.log("Error reading stopwords file", e);
                throw new RuntimeException("Error reading stopwords file");
            }
        });

        /*
         *  Rimozione di una stopword selezionata
         */
        Button btnRemove = new Button("Rimuovi selezionata");
        btnRemove.setOnAction(_ -> {
            String selected = sw.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    service.deleteStopword(selected);
                    sw.getItems().remove(selected);
                    isSWChanged.set(true);
                } catch (Exception ex) {
                    SystemLogger.log("Errore durante la rimozione di una stopword", ex);
                }
            }
        });

        btnAdd.defaultButtonProperty().bind(tf.focusedProperty());
        btnRemove.defaultButtonProperty().bind(tf.focusedProperty().not());
        tf.focusedProperty().addListener((_, _, _) -> {
        });

        popup.addAll(
                new Label("Inserisci una nuova stopword:"),
                tf,
                btnAdd,
                new Label("Oppure caricala da file:"),
                btnFile,
                new Label("StopWords attuali:"),
                sw, btnRemove);

        // Ricalcolo le WDM alla chiusura del popup se sono state modificate le stopwords
        popup.getStage().setOnHidden(_ -> {
            if (isSWChanged.get()) {
                requestRecalculate();
            }
        });
        popup.show();
    }

    private void requestRecalculate() {
        if (isRecalculatingWDMs.compareAndSet(false, true)) {
            // Non c'è un ricalcolo in corso, parto subito
            threadPool.submit(() -> {
                try {
                    service.reCalculateWDMs();
                } finally {
                    Platform.runLater(() -> {
                        System.out.println("Ricalcolo WDM completato.");
                        isRecalculatingWDMs.set(false);
                        if (needsRecalculation.getAndSet(false)) {
                            requestRecalculate();
                        }
                    });
                }
            });
        } else {
            // C'è già un ricalcolo in corso, segno che ne serve un altro dopo
            needsRecalculation.set(true);
            System.out.println("Ricalcolo WDM già in corso. Richiesta di ricalcolo accodata.");
        }
    }


    /**
     * Metodo di inizializzazione automatico chiamato da JavaFX.
     * <p>
     * Imposta le informazioni iniziali dell'interfaccia: etichetta dell'utente, visualizzazione menu per gli admin,
     * tabella dei report di gioco e statistiche, e comportamenti grafici dinamici.
     */
    @FXML
    public void initialize() {
        // Posiziona il secondo StackPane a metà altezza dell'AnchorPane a forma di semicerchio
        anchorSemicerchio.heightProperty().addListener((_, _, newVal) -> {
            double centerY = (newVal.doubleValue() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });
        Platform.runLater(() -> {
            double centerY = (anchorSemicerchio.getHeight() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });


        var isAdmin = currentUser.isAdmin();
        if (!isAdmin) {
            adminPanel.setVisible(false);
        }

        usernameLabel.setText(currentUser.getName());

        livelloClm.setCellValueFactory(report -> new SimpleStringProperty(report.getValue().difficulty().toString()));
        punteggioClm.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().score()));
        tempoClm.setCellValueFactory(report -> {
            Duration dur = report.getValue().usedTime();
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

    @FXML
    public void close() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}