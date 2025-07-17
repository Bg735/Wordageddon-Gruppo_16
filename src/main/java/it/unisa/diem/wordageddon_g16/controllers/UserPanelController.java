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
import javafx.fxml.Initializable;
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
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Controller per la gestione della vista del pannello utente.
 * <p>
 * Gestisce la visualizzazione delle statistiche personali dell’utente,
 * la gestione dei documenti, delle stopwords e, se l’utente ha privilegi amministrativi,
 * fornisce gli strumenti per la gestione degli utenti.
 * </p>
 */
public class UserPanelController implements Initializable {
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
    /**
     * Riferimento all’utente attualmente autenticato nell'applicazione.
     */
    private final User currentUser;
    /**
     * Riferimento al contesto applicativo che fornisce accesso ai servizi e alle risorse condivise.
     */
    private final AppContext appContext;

    /**
     * Indica se è in corso un ricalcolo delle WDM, per evitare concorrenza.
     */
    private final AtomicBoolean isRecalculatingWDMs;
    /**
     * Se 'true', segnala che un altro ricalcolo WDM è stato richiesto
     * mentre uno era già in esecuzione.
     */
    private final AtomicBoolean needsRecalculation;

    /**
     * Thread pool utilizzato per il ricalcolo parallelo delle WDM.
     * Ogni thread esegue un task di aggiornamento della WDM di una specifica WDM.
     * Per evitare concorrenza indesiderata, tali thread sono usati solo per il calcolo delle WDM
     */
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

    /**
     * Apre un popup per la gestione dei ruoli degli utenti presenti nel sistema.
     * <p>
     * Viene visualizzata una lista di utenti (escluso l'attuale tramite {@link UserPanelService#getAllUsersExceptCurrent()}), ognuno dei quali
     * può essere promosso o degradato tramite un {@code ToggleButton}.
     * L’aggiornamento del ruolo viene effettuato tramite chiamate a
     * {@link UserPanelService#promoteUser(String)}  o {@link UserPanelService#demoteUser(String)}.
     * <p>
     */
    @FXML
    private void handleAdmin() {
        Popup popup = new Popup("Gestione Ruoli Utenti");
        List<User> otherUsers = service.getAllUsersExceptCurrent();


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
        if (otherUsers.isEmpty()) {
            Label noUsersLabel = new Label("Nessun altro utente disponibile.");
            noUsersLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            popup.addAll(noUsersLabel);
            popup.show();
            return;
        }
        popup.show();
    }


    /**
     * Apre un popup per la gestione dei documenti testuali.
     * <p>
     * Funzionalità offerte:
     * <ul>
     *   <li>Visualizzazione di tutti i documenti salvati nel database.</li>
     *   <li>Rimozione di documenti esistenti.</li>
     *   <li>Caricamento di nuovi documenti con estensione ".txt" tramite {@link FileChooser}.</li>
     * </ul>
     * <p>
     * Dopo il caricamento, viene avviato in background il calcolo automatico della WDM associata, tramite
     * {@link UserPanelService#updateWDM(WDM)}.
     * </p>
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
                    service.moveDocument(selectedFile);
                    Document tempDoc = new Document(selectedFile.getName(), (titleTF.getText().isEmpty() ? service.symbolicNameOf(selectedFile.getName()) : titleTF.getText()), null);
                    documentList.add(tempDoc);

                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() {
                            // Creo la WDM e la inserisco nel database
                            // l'update inserisce anche il documento associato se non esiste
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
     * Apre un popup che permette la gestione delle stopwords.
     * <p>
     * Le funzionalità offerte includono:
     * <ul>
     * <li> Aggiunta manuale tramite un {@code TextField }</li>
     * <li> Caricamento da file di solo tipo '.txt'</li>
     * <li>Rimozione selezionata di stopwords da una {@code ListView }</li>
     * </ul>
     * <p>
     * Se viene rilevata una modifica alle stopwords, al termine della finestra
     * viene avviato automaticamente il ricalcolo di tutte le WDM associate
     * ai documenti esistenti nel sistema in modo ascrincono attraverso il thread pool
     * <p>
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
                if (isRecalculatingWDMs.compareAndSet(false, true)) {
                    // Nessun ricalcolo in corso: parte subito il ricalcolo
                    reCalculateWDMs();
                } else {
                    // Ricalcolo in corso
                    needsRecalculation.set(true);
                    System.out.println("Ricalcolo WDM già in corso. Richiesta di ricalcolo accodata.");
                }
            }
        });
        popup.show();
    }

    /**
     * Avvia il ricalcolo parallelo delle {@link WDM} (Word Document Matrix) per tutti i documenti
     * registrati nel database.
     * <p>
     * Viene creato un {@link Task} per ogni documento, eseguito tramite un {@link ExecutorService} .
     * Ogni task aggiorna la matrice WDM associata invocando
     * {@link UserPanelService#updateWDM(WDM)}.
     * <p>
     * Il metodo assicura che il ricalcolo non avvenga in parallelo ad altri ricalcoli
     * tramite il flag {@code isRecalculatingWDMs}, mentre eventuali richieste successive
     * vengono accodate tramite {@code needsRecalculation}.
     */
    private void reCalculateWDMs() {
        System.out.println("Rilevata cancellazione di una stopword, ricalcolo la WDM per tutti i documenti...");
        List<Document> allDocs = service.getAllDocuments().stream().toList();
        if (allDocs.isEmpty()) {
            System.out.println("Nessun documento presente... Non é necessario ricalcolare le stopwords");
            completeRecalculation();
            return;
        }

        // Lista dei task
        List<Callable<Void>> taskList = new ArrayList<>();
        for (Document doc : allDocs) {
            taskList.add(() -> {
                service.updateWDM(new WDM(doc, service.getStopwords()));
                return null;
            });
        }

        threadPool.submit(() -> {
            try {
                threadPool.invokeAll(taskList);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                Platform.runLater(this::completeRecalculation);
            }
        });
    }

    /**
     * Metodo di callback chiamato al termine del ricalcolo delle WDM.
     * <p>
     * Ripristina il flag {@code isRecalculatingWDMs} a 'false'. Se durante l’esecuzione
     * erano state richieste ulteriori modifiche (es. nuove stopwords), il metodo
     * ne avvia automaticamente un nuovo ricalcolo.
     */
    private void completeRecalculation() {
        System.out.println("Ricalcolo WDM completato.");
        isRecalculatingWDMs.set(false);
        if (needsRecalculation.getAndSet(false)) {
            // Se nel frattempo è stata fatta un'altra richiesta, riparti!
            if (isRecalculatingWDMs.compareAndSet(false, true)) {
                reCalculateWDMs();
            }
        }
    }

    /**
     * Metodo di inizializzazione del controller, invocato automaticamente da JavaFX.
     * <p>
     * Imposta i contenuti iniziali dell’interfaccia utente, tra cui:
     * <ul>
     *  <li>Nome utente</li>
     *  <li>Visualizzazione dell' adminPanel solo se l’utente è un amministratore </li>
     *  <li> Popolamento della tabella {@code userTableView} con i {@link GameReport} tramite la chiamata al metodo {@link UserPanelService#getCurrentUserReports()}</li>
     *  <li>Calcolo attraverso il metodo {@link UserPanelService#getCurrentUserReports()} e visualizzazione delle statistiche dell’utente corrente come punteggio massimo, media, numero di partite giocate
     * </ul>
     * <p>
     */
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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

    /**
     * Metodo di chiusura del controller.
     * <p>
     * Arresta il {@code threadPool} usato per eseguire in background i task
     * di ricalcolo delle WDM. Il metodo attende la terminazione dei thread
     * attivi per un massimo di 5 secondi.
     * Se l’attesa viene interrotta, forza la chiusura con shutdownNow().
     */
    @FXML
    public void close() {
        System.out.println("Chiusura del controller UserPanelController...");
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}