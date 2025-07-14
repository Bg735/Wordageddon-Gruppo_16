/**
 * @file GameSessionController.java
 * @brief Controller della sessione di gioco per l'applicazione Wordageddon.
 * @author Gruppo16
 */
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
 * @class GameSessionController
 * @brief Controller principale per la gestione di una sessione di gioco.
 * Gestisce la logica dell'interazione con l'utente durante la partita:
 * selezione difficoltà, lettura dei documenti, quiz a domande multiple
 * e resoconto finale con punteggio e tabella delle risposte date.
 *
 * Utilizza servizi JavaFX asincroni per mantenere la UI reattiva durante
 * operazioni intensive (generazione domande, caricamento testo).
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


    @FXML private TableView<Map.Entry<Question, Integer>> answersTable;
    @FXML private HBox actionBarBox;
    @FXML private Label wrongValue;
    @FXML private Text scoreValue;
    @FXML private VBox heroBox;
    @FXML private VBox answersBox;
    @FXML private StackPane mainStack;
    @FXML private Label viewAnswersBtnText;
    @FXML private Label rightValue;
    @FXML private Label completionValue;
    @FXML private AnchorPane reportPane;
    @FXML private Label questionNumber;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> domandaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> punteggioCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaCorrettaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaDataCln;
    @FXML private StackPane leaderboardBtn;
    @FXML private VBox questionContainer;
    @FXML private StackPane showAnswersBtn;
    @FXML private StackPane menuBtn;
    @FXML private VBox answerBox;
    @FXML private StackPane playAgainBtn;
    @FXML private Button backButtonDiff;
    @FXML private VBox difficultyButtonsBox;

    private Map<Document, String> documentToTextMap;
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
    private Map<Question, Integer> domandaRisposte; // Map con chiave la domanda e value l'indice della risposta data
    private final AppContext appContext;
    private LocalDateTime questionStartTime; // Rappresenta il momento in cui viene visualizzata la prima domanda
    private BooleanProperty questionsReady; // Indica se il thread di setup delle domande ha finito
    private BooleanProperty minTimeElapsed; // Secondi minimi prima di poter saltare la lettura
    private static final int MIN_TIME_FOR_SKIP = 1; // tempo minimo per skippare la lettura
    private static final java.time.Duration QUESTION_TIME_LIMIT = java.time.Duration.ofSeconds(10) ; //Tempo massimo per rispondere ad una domanda

    /**
     * @brief Costruttore della classe GameSessionController.
     *
     * Inizializza le proprietà tracciano lo stato della sessione di gioco,
     * come l'indice del documento corrente,l'indice della domanda mostrata, il tempo trascorso, e lo stato
     * delle domande. Recupera inoltre un'istanza del GameService dal contesto applicativo
     * fornito, così da poter accedere alla logica di gioco condivisa.
     *
     * @param[in] appContext Contesto applicativo contenente i servizi condivisi, incluso GameService.
     * @see AppContext
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
     * @brief Inizializza la sessione di gioco e configura i servizi asincroni.
     *
     * Questo metodo viene chiamato automaticamente da JavaFX al momento del caricamento del GameSessionController.
     *
     * In particolare:
     * - Carica la vista per la selezione della difficoltà
     * - Configura il pulsante di skip, presente durante lettura dei documenti, in modo che venga abilitato automaticamente quando la generazione delle domande è completata ('questionsReady')
     *  e sono trascorsi almeno 15s dall'inizio del timer ('minTimeElapsed')
     * - Istanzia e avvia 'readingSetupService' per la lettura dei documenti: viene eseguita in modo asincrono
     *  la generazione del testo da leggere e del tempo di lettura tramite il metodo setupReadingPhase del GameService
     * - Istanzia 'questionSetupService' per generare le domande del quiz in modalità asincrona tramite il metodo getQuestions del GameService
     * - Aggiorna lo stato 'questionsReady' al completamento
     *
     * @see GameService#setupReadingPhase()
     * @see GameService#getQuestions()
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
                    java.time.Duration seconds = gameService.getTimeLimit();
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
            throw new RuntimeException("Error during reading setup task: " + questionSetupService.getException());
        });

        loadPane(diffSelectionPane);
    }

    /**
     * @brief Mostra il contenuto del documento corrente nell'area di lettura.
     *
     * @param[in] i Indice del documento da visualizzare.
     */
    private void setDocument(int i) {
        Document doc = gameService.getDocuments().get(i);
        documentTitleLabel.setText(doc.title());
        textDisplayArea.setText(documentToTextMap.get(doc));
    }

    /**
     * @brief Avvia la fase del quiz dopo la lettura.
     *
     * Se le domande non sono ancora pronte, aspetta un secondo e riprova.
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
     * @brief Visualizza una domanda e le sue risposte nella UI.
     *
     * Gestisce la risposta dell'utente, il controllo correttezza,
     * e avanza alla prossima domanda o al report finale se non ci sono piu domande.
     *
     * @param[in] index Indice della domanda da mostrare.
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

    /**
     * @brief Genera il report di fine partita e aggiorna la UI.
     *
     * Calcola il tempo impiegato per rispondere alle domande, crea e salva il 'GameReport' registrato, aggiorna le statistiche della nuova vista (score,
     * risposte corrette, risposte sbagliate, percentuale di risposte esatte su quelle date)
     * e popola la tabella che mostra il resoconto della partita tramite il metodo populateAnswerTable() del controller.
     *
     * @see @GameSessionController#populateAnswerTable()
     */
    private void showReport() {
        LocalDateTime questionEndTime = LocalDateTime.now();
        java.time.Duration usedTime = java.time.Duration.between(questionStartTime, questionEndTime);
        java.time.Duration timeLimit = QUESTION_TIME_LIMIT.multipliedBy(gameService.getQuestionCount());

        GameReport report = new GameReport(
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
     * @brief Avvia un timer countdown con aggiornamento su barra e label.
     *
     * Mostra il tempo residuo, aggiorna il colore della barra in base al tempo rimasto
     * e chiama una funzione al termine del countdown.
     *
     * @param[in] duration Durata totale del timer.
     * @param[in] label Label da aggiornare con il tempo rimanente.
     * @param[in] bar ProgressBar da aggiornare.
     * @param[in] onFinished Callback da eseguire al termine del timer.
     * @return Istanza di Timeline attiva per il timer.
     */
    private Timeline startTimer(java.time.Duration duration, Label label, ProgressBar bar, Runnable onFinished) {
        int totalSeconds = (int) duration.getSeconds();

        if (totalSeconds <= 0) {
            bar.setProgress(1.0);
            label.setText("00:00");
            onFinished.run();
            return null;
        }

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

        for (int i = 0; i <= totalSeconds; i++) {
            int secondsRemaining = totalSeconds - i;
            double progress = (double) i / totalSeconds;
            timer.getKeyFrames().add(new KeyFrame(javafx.util.Duration.seconds(i), _ -> {
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

        updateBarStyle.run(); // Imposta colore iniziale
        timer.play();
        return timer;
    }

    /**
     * @brief Cambia il pannello visibile nello StackPane principale.
     *
     * Esegue azioni specifiche a seconda del pannello mostrato (es. avvio lettura, avvia domande, mostra risultati).
     *
     * @param[in] pane Nodo FXML da rendere visibile.
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
     * @brief Gestisce la selezione della difficoltà da parte dell'utente.
     *
     * Metodo chiamato quando l'utente seleziona un livello di difficoltà tramite pulsante.
     * In base all'ID del pulsante cliccato inizializza il GameService con il livello di difficoltà corrispondente
     * Avvia la fase di lettura dei documenti caricando il relativo pannello ('readingPane')
     *
     * @param event Evento generato dalla selezione di difficoltà (ActionEvent su pulsante)
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
     * @brief Ritorna alla schermata principale del menu.
     */
    @FXML
    private void onBackPressed() {
        //Torna al menu principale o chiudi la finestra
        ViewLoader.load(ViewLoader.View.MENU);
    }

    /**
     * @brief Cambia il documento visualizzato nella fase di lettura.
     *
     * @param[in] event Evento generato dai pulsanti "Successivo" o "Precedente".
     */
    @FXML
    private void onChangeDocument(ActionEvent event) {
        if (event.getSource().equals(nextDocumentButton)) {
            currentDocumentIndex.set(currentDocumentIndex.get() + 1);
        } else { // previousDocumentButton pressed
            currentDocumentIndex.set(currentDocumentIndex.get() - 1);
        }
        setDocument(currentDocumentIndex.get());
    }

    /**
     * @brief Salta la lettura dei documenti e passa direttamente alle domande.
     */
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
        viewAnswersBtnText.setText((heroBox.isVisible() ? "Mostra Resoconto" : "Mostra Risposte"));
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
     *@brief Popola la tabella delle risposte visibile a fine partita.
     *
     * Il metodo viene chiamato in {@code showReport()} per visualizzare il riepilogo
     * delle risposte date dall'utente. Utilizza la mappa 'domandaRisposte'
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
