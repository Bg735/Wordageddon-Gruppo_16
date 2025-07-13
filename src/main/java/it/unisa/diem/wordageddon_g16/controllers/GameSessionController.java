package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    @FXML private Button diffEasyBTN;
    @FXML private Button diffMediumBTN;
    @FXML private Button diffHardBTN;


    @FXML private TextArea textDisplayArea;
    @FXML private ProgressBar timerBar;
    @FXML private Label timerLabelRead;
    @FXML private Label documentTitleLabel;
    @FXML private Button nextDocumentButton;
    @FXML private Button previousDocumentButton;
    @FXML private Button skipReadingBtn;

    @FXML private Label questionText;
    @FXML private Label questionCountLabel;
    @FXML private ProgressBar timerBarQuestion;
    @FXML private Label timerLabelQuestion;
    @FXML private Button answer1Btn;
    @FXML private Button answer2Btn;
    @FXML private Button answer3Btn;
    @FXML private Button answer4Btn;





    Map<Document, String> documentToTextMap;

    private SimpleIntegerProperty currentQuestionIndex;
    private final SimpleIntegerProperty currentDocumentIndex;
    private final GameService gameService;
    private List<Question> questions;

    private Service<Map<Document, String>> readingSetupService;
    private Service<List<Question>> questionSetupService;
    private Timeline questionTimer;

    private final SimpleIntegerProperty elapsedSeconds;
    private Timeline readingTimer;
    private int score = 0;
    private int numeroRisposteCorrette = 0;
    private int numeroRisposteSaltate = 0;
    private Map<Question, Integer> domandaRisposte;


    private final AppContext appContext;

    private LocalDateTime questionStartTime;

    // Indica se il thread di setup delle domande ha finito
    private BooleanProperty questionsReady;
    private BooleanProperty minTimeElapsed;

    // Secondi minimi prima di poter saltare la lettura
    private static final int MIN_TIME_FOR_SKIP = 1;
    private static final int QUESTION_TIME_LIMIT = 10;

    @FXML private TableView<Map.Entry<Question, Integer>> answersTable;
    @FXML
    private HBox actionBarBox;
    @FXML
    private Label wrongValue;
    @FXML
    private Text scoreValue;
    @FXML
    private VBox heroBox;
    @FXML
    private VBox answersBox;
    @FXML
    private StackPane mainStack;
    @FXML
    private Label viewAnswersBtnText;
    @FXML
    private Label rightValue;
    @FXML
    private Label completionValue;
    @FXML
    private AnchorPane reportPane;
    @FXML
    private Label questionNumber;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> domandaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> punteggioCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaCorrettaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaDataCln;



    /**
     * Costruisce il controller e inizializza il servizio di gioco.
     *
     * @param appContext il contesto applicativo che fornisce il GameService da utilizzare per la sessione.
     */
    public GameSessionController(AppContext appContext) {
        this.gameService = appContext.getGameService();
        this.appContext= appContext;
        currentDocumentIndex = new SimpleIntegerProperty(0);
        currentQuestionIndex = new SimpleIntegerProperty(0);
        elapsedSeconds = new SimpleIntegerProperty(0);
        questionsReady = new SimpleBooleanProperty(false);
        minTimeElapsed = new SimpleBooleanProperty(false);
        domandaRisposte = new LinkedHashMap<>();

    }

    /**
     * Inizializza il controller dopo il caricamento della view FXML.
     * Avvia la fase di lettura e imposta la schermata iniziale sulla selezione della difficoltà.
     */
    @FXML
    public void initialize() {
        // IL pulsante di skip viene abilitato automaticamente quando la generazione delle domande è completata e sono trascorsi almeno 15s dall'inizio del timer
        skipReadingBtn.disableProperty().bind(
                minTimeElapsed.not().or(questionsReady.not())
        );

        //Instanziazione dei servizi per la generazione asincrona del testo e delle domande

        /*
         * Avvia la fase di lettura dei documenti in un servizio asincrono.
         * Alla conclusione, aggiorna la UI con il testo letto, avvia la generazione delle domande
         * e imposta il timer per la lettura.
         */
        readingSetupService = new Service<>() {
            @Override
            protected Task<Map<Document, String>> createTask() {
                Task<Map<Document, String>> task = new Task<>() {
                    @Override
                    protected Map<Document, String> call() {
                        return gameService.setupReadingPhase();
                    }
                };
                task.setOnSucceeded(_ -> {
                    documentToTextMap = task.getValue();
                    Platform.runLater(() -> setDocument(0));

                    questionSetupService.start();
                    int seconds = (int) gameService.getTimeLimit().getSeconds();
                    readingTimer = startTimer(seconds, timerLabelRead, timerBar, () -> loadPane(questionPane));

                    // Avvia la pausa per abilitare lo skip dopo 15 secondi
                    PauseTransition wait15s = new PauseTransition(Duration.seconds(MIN_TIME_FOR_SKIP));
                    wait15s.setOnFinished(_ -> minTimeElapsed.set(true));
                    wait15s.play();
                });
                task.setOnFailed(_ -> {
                    throw new RuntimeException("Error during reading setup task", task.getException());
                });
                return task;
            }
        };


        /*
         * Avvia la generazione delle domande del quiz in un servizio asincrono.
         * Quando la generazione è completata, aggiorna la lista delle domande e imposta il timer per le domande.
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
            questionsReady.set(true);  // Le domande sono pronte
        });
        questionSetupService.setOnFailed(_ -> {
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
        if (questions == null) {
            // Le domande non sono ancora pronte: aspetta e riprova tra poco
            PauseTransition wait = new PauseTransition(Duration.seconds(1));
            wait.setOnFinished(_ -> switchToQuestions());
            wait.play();
            return;
        }
        if (questions.isEmpty()) {
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
        //Se il l'indice della prossima domanda da visualizzare è maggiore del numero di domande, viene chiamato loadPane(reportPane)
        if (currentQuestionIndex.get() >= questions.size()) {
            loadPane(reportPane);
            return;
        }
        // alla prima domanda da mostrare, viene segnato il tempo nella variabile questionStartTime.
        // in showReport verrà calcolato il tempo che intercorre tra questionStartTime e questionEndTime
        if (currentQuestionIndex.get() == 0) {
            questionStartTime = LocalDateTime.now();
        }

        Question q = questions.get(index);
        questionText.setText(q.text());
        questionCountLabel.setText((index + 1) + "/" + questions.size());
        List<String> answers = q.answers();
        Button[] buttons = {answer1Btn, answer2Btn, answer3Btn, answer4Btn};
        // Avvia il timer tramite metodo startTimer
        if(questionTimer != null) {
            questionTimer.stop();
        }
        questionTimer = startTimer(QUESTION_TIME_LIMIT, timerLabelQuestion, timerBarQuestion, () -> Platform.runLater(() -> {
            domandaRisposte.put(q, -1);
            // Alla fine del timer, se non è stata data risposta, mostra la risposta corretta
            numeroRisposteSaltate++;
            System.out.println("Risposta saltata. Numero risposte saltate: " + numeroRisposteSaltate);
            // Disabilita tutti i pulsanti
            for (Button b : buttons) {
                b.setDisable(true);
            }
            // Evidenzia la risposta corretta
            int correctIndex = q.correctAnswerIndex();
            if (correctIndex >= 0 && correctIndex < buttons.length) {
                buttons[correctIndex].setStyle("-fx-background-color: #4CAF50;");
            }
            // Dopo 0.5s, mostra la prossima domanda
            PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
            pause.setOnFinished(_ -> {
                currentQuestionIndex.set(currentQuestionIndex.get() + 1);
                showQuestion(currentQuestionIndex.get());
            });
            pause.play();
        }));

        // Reset stile e stato dei bottoni
        for (Button b : buttons) {
            b.setStyle("");
            b.setDisable(false);
        }
        // Mostra solo i bottoni necessari
        for (int i = 0; i < answers.size(); i++) {
            Button btn = buttons[i];
            String capitalizedAnswer = answers.get(i).substring(0, 1).toUpperCase() + answers.get(i).substring(1);
            btn.setText(capitalizedAnswer);
            btn.setVisible(true);
            final int answerIndex = i;
            // GESTIONE EVENTO OnClick su btn che mostra risposta alternativa
            btn.setOnAction(_ -> {
                domandaRisposte.put(q, answerIndex); //Salva domanda e risposta
                // Stoppa il timer se in corso
                if (questionTimer != null) {
                    questionTimer.stop();
                    questionTimer = null;
                }
                // Verifica risposta
                boolean isCorrect = answerIndex == q.correctAnswerIndex();
                if (isCorrect) {
                    btn.setStyle("-fx-background-color: #4CAF50;");
                    score += gameService.getScorePerQuestion();
                    numeroRisposteCorrette++;
                    System.out.println("\nScore: " + score);
                } else {
                    btn.setStyle("-fx-background-color: #F44336;");
                    buttons[q.correctAnswerIndex()].setStyle("-fx-background-color: #4CAF50;");
                }
                // Disabilita tutti i bottoni
                for (Button b : buttons) {
                    b.setDisable(true);
                }
                // Pausa di 0.5s prima di mostrare la prossima domanda
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(_ -> {
                    currentQuestionIndex.set(currentQuestionIndex.get() + 1);
                    showQuestion(currentQuestionIndex.get());
                });
                pause.play();
            });
        }

    }

    private void showReport() {
        LocalDateTime questionEndTime = LocalDateTime.now();
        java.time.Duration usedTime = java.time.Duration.between(questionStartTime, questionEndTime);
        java.time.Duration timeLimit = gameService.getTimeLimit().multipliedBy(gameService.getQuestionCount());

        GameReport report = new GameReport(
                0, // ID generato dal DB
                appContext.getCurrentUser(),
                gameService.getDocuments(),
                LocalDateTime.now(),
                gameService.getDifficulty(),
                timeLimit,
                usedTime,
                gameService.getQuestionCount(),
                score
        );
        gameService.saveGameReport(report);
        scoreValue.setText(String.valueOf(score));
        rightValue.setText(String.valueOf(numeroRisposteCorrette));
        wrongValue.setText(String.valueOf(gameService.getQuestionCount() - numeroRisposteCorrette));
        questionNumber.setText(String.valueOf(gameService.getQuestionCount()));

        int numeroDomandeTotali = gameService.getQuestionCount();
        int numeroRisposteDate = numeroDomandeTotali - numeroRisposteSaltate;
        double percentualeCompletamento = (double) numeroRisposteDate / numeroDomandeTotali  * 100;

        completionValue.setText(String.format("%.2f%%", percentualeCompletamento));
        populateAnswerTable();
    }

    /**
     * Avvia un timer visuale che aggiorna una label e una progress bar ogni secondo.
     * Al termine del conto alla rovescia, esegue la callback specificata.
     *
     * @param durationSeconds durata del timer in secondi
     * @param label           label da aggiornare con il tempo rimanente
     * @param bar             progress bar da aggiornare con l'avanzamento del tempo
     * @param onFinished      operazione da eseguire al termine del timer
     * @return oggetto Timeline che rappresenta il timer in esecuzione
     */
    private Timeline startTimer(int durationSeconds, Label label, ProgressBar bar, Runnable onFinished) {
        Runnable updateBarStyle = () -> Platform.runLater(() -> {
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

        Timeline timer = new Timeline();
        for (int i = 0; i <= durationSeconds; i++) {
            int secondsRemaining = durationSeconds - i;
            double progress = (double) i / durationSeconds;
            timer.getKeyFrames().add(new KeyFrame(Duration.seconds(i), _ -> {
                label.setText(String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60));
                Platform.runLater(() -> {
                    bar.setProgress(progress);
                    updateBarStyle.run();
                });
                elapsedSeconds.set(elapsedSeconds.get() + 1);
                if (secondsRemaining == 0) {
                    onFinished.run();
                }
            }));
        }
        bar.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                Platform.runLater(updateBarStyle);
            }
        });
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
        for (Node p : stackPane.getChildren()) {
            p.setVisible(false);
        }
        switch (pane.getId()) {
            case "readingPane" -> readingSetupService.start();
            case "questionPane" -> switchToQuestions();
            case "reportPane" -> showReport();
            default -> {
            }

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
        switch (((Button) event.getSource()).getId()) {
            case "diffEasyBTN" -> gameService.init(Difficulty.EASY);
            case "diffMediumBTN" -> gameService.init(Difficulty.MEDIUM);
            case "diffHardBTN" -> gameService.init(Difficulty.HARD);
            default -> throw new IllegalArgumentException("Difficoltà non riconosciuta");
        }
        nextDocumentButton.disableProperty().bind(currentDocumentIndex.isEqualTo(gameService.getDocuments().size() - 1));
        previousDocumentButton.disableProperty().bind(currentDocumentIndex.isEqualTo(0));
        loadPane(readingPane);
    }

    /**
     * Gestisce il ritorno al menu principale o la chiusura della finestra corrente.
     *
     */
    @FXML
    private void onBackPressed() {
        //Torna al menu principale o chiudi la finestra
        ViewLoader.load(ViewLoader.View.MENU);
    }

    @FXML
    private void onChangeDocument(ActionEvent event) {
        if (event.getSource().equals(nextDocumentButton)) {
            currentDocumentIndex.set(currentDocumentIndex.get() + 1);
        } else { // previousDocumentButton pressed
            currentDocumentIndex.set(currentDocumentIndex.get() - 1);
        }
        setDocument(currentDocumentIndex.get());
    }

    @FXML
    public void skipReading() {
        // Ferma il timer di lettura se è attivo
        if (readingTimer != null) {
            readingTimer.stop();
        }
        // Passa subito alla fase delle domande
        loadPane(questionPane);
    }

    /**
     * Gestisce il click sul pulsante "Leaderboard" del presente nella pagina dei Risultati.
     * Carica la schermata della leaderboard utilizzando il {@code ViewLoader}.
     *
     * @param event l'evento generato dal click dell'utente
     */
    @FXML
    public void handleShowLeaderboard(Event event) {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }


    /**
     * Alterna la visualizzazione della sezione dei Report e della TableView popolata tramite il metodo {@code populateAnswerTable}.
     *
     * @param event l'evento generato dal click sul pulsante di toggle
     */
    @FXML
    public void toggleShowAnswers(Event event) {
        heroBox.setVisible(!heroBox.isVisible());
        answersBox.setVisible(!answersBox.isVisible());



    }
    /**
     * Gestisce il click sul pulsante "Menu" del presente nella pagina dei Risultati.
     * Carica la schermata del Menu utilizzando il {@code ViewLoader}.
     *
     * @param event l'evento generato dal click dell'utente
     */
    @FXML
    public void handleGoMenu(Event event) {
        ViewLoader.load(ViewLoader.View.MENU);
    }
    /**
     * Gestisce il click sul pulsante "Play Again" del presente nella pagina dei Risultati.
     * Carica la schermata del Game utilizzando il {@code ViewLoader}.
     *
     * @param event l'evento generato dal click dell'utente
     */
    @FXML
    public void handlePlayAgain(Event event) {
        ViewLoader.load(ViewLoader.View.GAME);
    }

    /**
     * Popola la tabella delle risposte alla fine della partita.
     *
     * Il metodo viene chiamato in {@code showReport()} per visualizzare il riepilogo
     * delle risposte date dall'utente. Utilizza la mappa {@code domandaRisposte}
     * per mostrare, per ogni domanda:
     * <ul>
     *   <li>Il testo della domanda</li>
     *   <li>La risposta data dall'utente (o "Saltata" se assente)</li>
     *   <li>La risposta corretta</li>
     *   <li>Il punteggio ottenuto per la risposta</li>
     * </ul>
     */
    private void populateAnswerTable() {
        answersTable.getItems().addAll(domandaRisposte.entrySet());
        // Colonna Domanda
        domandaCln.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey().text())
        );
        // Colonna Risposta Data
        rispostaDataCln.setCellValueFactory(data -> {
            Question question = data.getValue().getKey();
            int givenAnswerIndex = data.getValue().getValue();
            String answer = (givenAnswerIndex == -1) ? "Saltata" : question.answers().get(givenAnswerIndex);
            return new SimpleStringProperty(answer);
        });
        // Colonna Risposta Corretta
        rispostaCorrettaCln.setCellValueFactory(data -> {
            Question question = data.getValue().getKey();
            int correctIndex = question.correctAnswerIndex();
            String correctAnswer = question.answers().get(correctIndex);
            return new SimpleStringProperty(correctAnswer);
        });
        // Colonna Punteggio
        punteggioCln.setCellValueFactory(data -> {
            int givenIndex = data.getValue().getValue();
            int score = (givenIndex == -1) ? 0 : (givenIndex == data.getValue().getKey().correctAnswerIndex() ? gameService.getScorePerQuestion() : 0); //Se Integer è -1 allora la domanda è saltata, se l'indice della risposta data è lo stesso della risposta corretta allora stampo il punteggio altrimenti 0
            return new SimpleStringProperty(String.valueOf(score));
        });

        punteggioCln.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Map.Entry<Question, Integer> entry = getTableView().getItems().get(getIndex());
                    if ("0".equals(item)) {
                        int givenIndex = entry.getValue();
                        if (givenIndex == -1) {
                            setStyle("-fx-background-color: #fff3b0;"); // se la domanda è stata saltata allora colore giallo
                        } else {
                            setStyle("-fx-background-color: #ffcccc;"); //domanda sbagliata colore rosso
                        }
                    } else {
                        setStyle(""); //corrette
                    }
                }
            }
        });

    }


}
