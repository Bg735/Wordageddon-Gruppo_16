package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * Controller JavaFX responsabile della gestione della sessione di gioco.
 * <p>
 * Coordina la visualizzazione delle varie fasi della partita:
 * selezione difficoltà, lettura documenti, domande e risposte.
 * Utilizza servizi asincroni per operazioni di I/O e generazione domande,
 * per mantenere la UI reattiva.
 */
public class GameSessionController {
    @FXML private StackPane stackPane;
    @FXML private AnchorPane readingPane;
    @FXML private AnchorPane questionPane;
    @FXML private AnchorPane diffSelectionPane;

    @FXML private TextArea textDisplayArea;
    @FXML private ProgressBar timerBar;
    @FXML private Label timerLabelRead;
    @FXML private Label documentTitleLabel;

    @FXML private Label questionText;
    @FXML private VBox answerBox;
    @FXML private Label questionCountLabel;
    @FXML private ProgressBar timerBarQuestion;
    @FXML private Label timerLabelQuestion;
    @FXML private Button nextQuestionButton;
    @FXML private Button nextDocumentButton;
    @FXML private Button previousDocumentButton;

    Map<Document,String> documentToTextMap;
    private SimpleIntegerProperty currentQuestionIndex;
    private final SimpleIntegerProperty currentDocumentIndex;
    private final GameService gameService;
    private List<Question> questions;

    private Timeline questionTimer;

    private Service<Map<Document,String>> readingSetupService;
    private Service<List<Question>> questionSetupService;

    /**
     * Costruisce il controller e inizializza il servizio di gioco.
     *
     * @param appContext il contesto applicativo che fornisce il GameService da utilizzare per la sessione.
     */
    public GameSessionController(AppContext appContext) {
        this.gameService = appContext.getGameService();
        currentDocumentIndex = new SimpleIntegerProperty(0);
    }

    /**
     * Inizializza il controller dopo il caricamento della view FXML.
     * Avvia la fase di lettura e imposta la schermata iniziale sulla selezione della difficoltà.
     */
    @FXML
    public void initialize() {
        //Instanziazione dei servizi per la generazione asincrona del testo e delle domande

        /*
         * Avvia la fase di lettura dei documenti in un servizio asincrono.
         * Alla conclusione, aggiorna la UI con il testo letto, avvia la generazione delle domande
         * e imposta il timer per la lettura.
         */
        readingSetupService = new Service<>() {
            @Override
            protected Task<Map<Document,String>> createTask() {
                Task<Map<Document,String>> task= new Task<>() {
                    @Override
                    protected Map<Document,String> call() {
                        return gameService.setupReadingPhase();
                    }
                };
                task.setOnSucceeded(_ -> {
                    documentToTextMap= task.getValue();
                    Platform.runLater(
                            () -> {
                                setDocument(0);
                            }
                    );
                    questionSetupService.start();
                    int seconds = (int) gameService.getTimeLimit().getSeconds();
                    // alla fine del timer mostra questionPane
                    startTimer(seconds, timerLabelRead, timerBar, () -> loadPane(questionPane));
                });
                task.setOnFailed(_ -> {
                    endGame();
                    throw new RuntimeException("Erorr during reading setup task", task.getException());
                });
                return task;
            }
        };

        /*
         * Avvia la generazione delle domande del quiz in un servizio asincrono.
         * Quando la generazione è completata, aggiorna la lista delle domande e imposta il timer per la lettura.
         * In caso di errore, termina la sessione di gioco.
         */
        questionSetupService = new Service<>() {
            @Override
            protected Task<List<Question>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Question> call() {
                        return gameService.getQuestions();
                    }
                };
            }
        };
        questionSetupService.setOnSucceeded(_ -> {
            questions = questionSetupService.getValue();
            int seconds = (int) gameService.getTimeLimit().getSeconds();
            startTimer(seconds, timerLabelRead, timerBar, () -> loadPane(questionPane)); //TODO questo dovrebbe caricare il Pane coi risultati, non il questionPane
        });
        questionSetupService.setOnFailed(_ -> {
            endGame();
            throw new RuntimeException("Error during reading setup task");
        });

        loadPane(diffSelectionPane);
    }

    private void setDocument(int i) {
        Document doc = gameService.getDocuments().get(i);
        documentTitleLabel.setText(doc.title());
        textDisplayArea.setText(documentToTextMap.get(doc));
    }

    /**
     * Passa dalla fase di lettura a quella delle domande.
     * Se le domande non sono ancora pronte, attende e riprova.
     * Altrimenti, mostra la domanda corrente.
     */
    private void switchToQuestions() {
        if (questions.isEmpty()) {
            PauseTransition wait = new PauseTransition(Duration.seconds(1));
            wait.setOnFinished(e -> switchToQuestions());
            wait.play();
            return;
        }
        showQuestion(currentQuestionIndex.get());
    }

    /**
     * Visualizza la domanda corrente e genera i pulsanti delle possibili risposte.
     *
     * @param index indice della domanda da visualizzare nella lista delle domande.
     */
    private void showQuestion(int index) {
        if (index >= questions.size()) {
            endGame();
            return;
        }
        Question q = questions.get(index);
        questionText.setText(q.text());
        questionCountLabel.setText((index + 1) + "/" + questions.size());

        answerBox.getChildren().clear();
        List<String> answers = q.answers();

        for (int i = 0; i < answers.size(); i++) {
            int finalI = i;
            Button btn = new Button(answers.get(i));
            btn.getStyleClass().add("buttonAnswer");
            btn.setOnAction(e -> handleAnswer(finalI));
            answerBox.getChildren().add(btn);
        }
        //BISOGNA DECIDERE IL TEMPO DELLE DOMANDE COME GESTIRLO
        // Avvia il timer della domanda corrente. Se il tempo scade, passa automaticamente alla successiva.
        startTimer(20, timerLabelQuestion, timerBarQuestion, () -> {
            currentQuestionIndex.set(currentQuestionIndex.get() + 1);
            showQuestion(currentQuestionIndex.get());
        });
    }

    /**
     * Gestisce la selezione di una risposta da parte dell'utente.
     *
     * @param selectedIndex indice della risposta scelta dall'utente.
     */
    private void handleAnswer(int selectedIndex) {

    }

    /**
     * Termina la sessione di gioco e visualizza eventuali messaggi di fine partita.
     */
    private void endGame() {
        System.out.println("Game over");
    }

    /**
     * Avvia un timer visuale che aggiorna una label e una progress bar ogni secondo.
     * Al termine del conto alla rovescia, esegue la callback specificata.
     *
     * @param durationSeconds durata del timer in secondi
     * @param label label da aggiornare con il tempo rimanente
     * @param bar progress bar da aggiornare con l'avanzamento del tempo
     * @param onFinished operazione da eseguire al termine del timer
     * @return oggetto Timeline che rappresenta il timer in esecuzione
     */
    private Timeline startTimer(int durationSeconds, Label label, ProgressBar bar, Runnable onFinished) {

        label.setText(String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60));

        // Metodo interno per aggiornare lo stile della progress bar
        Runnable updateBarStyle = () -> {
            Platform.runLater(() -> {
                // lookup funziona solo se il nodo è già nel scene graph
                var barNode = bar.lookup(".bar");
                if (barNode != null) {
                    barNode.getStyleClass().removeAll("low", "medium", "high");
                    double progress = bar.getProgress();
                    if (progress < 0.3) {
                        barNode.getStyleClass().add("high");
                    } else if (progress < 0.7) {
                        barNode.getStyleClass().add("medium");
                    } else {
                        barNode.getStyleClass().add("low");
                    }
                }
            });
        };

        Timeline timer = new Timeline();
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            String[] parts = label.getText().split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            int totalSeconds = minutes * 60 + seconds - 1;
            if (totalSeconds < 0) totalSeconds = 0;

            label.setText(String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60));

            double progress = (double) (durationSeconds - totalSeconds) / durationSeconds;
            bar.setProgress(progress);
            updateBarStyle.run();

            if (totalSeconds <= 0) {
                timer.stop(); // <-- usa direttamente la variabile locale
                onFinished.run();
            }
        }));

        // Dopo che la ProgressBar è mostrata, applica lo stile iniziale
        bar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(updateBarStyle);
            }
        });

        timer.setCycleCount(durationSeconds);
        timer.play();
        return timer;
    }

    /**
     * Gestisce la visualizzazione dei diversi pannelli dell'interfaccia.
     * Rende visibile il pannello specificato e avvia eventuali servizi associati.
     *
     * @param pane il nodo (AnchorPane) da rendere visibile nello StackPane principale.
     */
    private void loadPane(Node pane) {
        for(Node p : stackPane.getChildren()) {
            p.setVisible(false);
        }
        switch(pane.getId()){
            case "readingPane" -> readingSetupService.start();
            case "questionPane" -> switchToQuestions();
            default -> {}
        }
        pane.setVisible(true);
    }

    /**
     * Gestisce la selezione della difficoltà da parte dell'utente.
     * Inizializza la partita con la difficoltà scelta e avvia la fase di lettura.
     *
     * @param event evento di selezione generato dal click su uno dei bottoni di difficoltà.
     */
    @FXML
    public void onDifficultySelected(ActionEvent event) {
        switch (((Button) event.getSource()).getId()){
            case "diffEasyBTN" -> gameService.init(Difficulty.EASY);
            case "diffMediumBTN" -> gameService.init(Difficulty.MEDIUM);
            case "diffHardBTN" -> gameService.init(Difficulty.HARD);
            default -> throw new IllegalArgumentException("Difficoltà non riconosciuta");
        }
        nextDocumentButton.disableProperty().bind(currentDocumentIndex.isEqualTo(gameService.getDocuments().size()-1));
        previousDocumentButton.disableProperty().bind(currentDocumentIndex.isEqualTo(0));
        loadPane(readingPane);
    }

    /**
     * Gestisce il ritorno al menu principale o la chiusura della finestra corrente.
     *
     * @param event evento generato dalla pressione del pulsante "Back".
     */
    @FXML
    private void onBackPressed(ActionEvent event) {
        //Torna al menu principale o chiudi la finestra
        ViewLoader.load(ViewLoader.View.MENU);
    }

    @FXML
    private void onChangeDocument(ActionEvent event) {
        if (event.getSource().equals(nextDocumentButton)) {
            currentDocumentIndex.set(currentDocumentIndex.get()+1);
        } else { // previousDocumentButton pressed
            currentDocumentIndex.set(currentDocumentIndex.get()-1);
        }
        setDocument(currentDocumentIndex.get());
    }
}
