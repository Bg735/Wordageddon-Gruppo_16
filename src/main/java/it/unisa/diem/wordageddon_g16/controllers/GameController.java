package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.models.Question;
import it.unisa.diem.wordageddon_g16.utility.Config;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller principale per la gestione di una sessione di gioco.
 * <p>
 * Gestisce la logica dell'interazione con l'utente durante la partita:
 * selezione difficoltà, lettura dei documenti, quiz a domande multiple
 * e resoconto finale con punteggio e tabella delle risposte date.
 * </p>
 * <p>
 * Utilizza servizi JavaFX asincroni per mantenere la UI reattiva durante
 * operazioni intensive (generazione domande, caricamento testo).
 * </p>
 */
public class GameController implements Initializable {
    @FXML private StackPane stackPane;
    @FXML private AnchorPane readingPane;
    @FXML private AnchorPane questionPane;
    @FXML private AnchorPane diffSelectionPane;


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
    @FXML private Label wrongValue;
    @FXML private Text scoreValue;
    @FXML private VBox heroBox;
    @FXML private VBox answersBox;
    @FXML private Label viewAnswersBtnText;
    @FXML private Label rightValue;
    @FXML private Label completionValue;
    @FXML private AnchorPane reportPane;
    @FXML private Label questionNumber;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> domandaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> punteggioCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaCorrettaCln;
    @FXML private TableColumn<Map.Entry<Question, Integer>, String> rispostaDataCln;


    /**
     * Mappa che associa ciascun documento al rispettivo testo caricato da mostrare all'utente nella fase di lettura.
     */
    private Map<Document, String> documentToTextMap;

    /**
     * Indice della domanda attualmente visualizzata dal quiz.
     * Aggiornato a ogni avanzamento tra le domande.
     */
    private SimpleIntegerProperty currentQuestionIndex;

    /**
     * Indice del documento attualmente visualizzato nella fase di lettura.
     * Utilizzato dai pulsanti "Successivo" e "Precedente" per navigare tra i documenti.
     */
    private final SimpleIntegerProperty currentDocumentIndex;

    /**
     * Servizio centrale che gestisce la logica di gioco (selezione difficoltà, generazione quiz, calcolo punteggi ecc).
     */
    private final GameService gameService;

    /**
     * Lista delle domande generate per la sessione corrente.
     */
    private List<Question> questions;

    /**
     * Servizio JavaFX asincrono per la fase di caricamento e visualizzazione dei documenti da leggere.
     */
    private Service<Map<Document, String>> readingSetupServiceFX;

    /**
     * Servizio JavaFX asincrono che genera in background le domande del quiz, mantenendo la UI reattiva.
     */
    private Service<List<Question>> questionSetupServiceFX;

    /**
     * Timeline per la gestione del conto alla rovescia del timer di risposta alle domande.
     */
    private Timeline questionTimer;

    /**
     * Conta il tempo trascorso dall'inizio della sessione, in secondi.
     * Utilizzato per statistiche o per il timer delle domande e della lettura.
     */
    private final SimpleIntegerProperty elapsedSeconds;

    /**
     * Timeline per il timer di lettura dei documenti da parte dell'utente.
     */
    private Timeline readingTimer;

    /**
     * Punteggio totale ottenuto dal giocatore nella sessione corrente.
     */
    private int score;

    /**
     * Numero di risposte corrette fornite dal giocatore.
     */
    private int numeroRisposteCorrette;

    /**
     * Numero di domande a cui l'utente non ha risposto (scaduto il tempo), ovvero saltate.
     */
    private int numeroRisposteSaltate;

    /**
     * Mappa che tiene traccia, per ogni domanda, dell'indice della risposta fornita dall'utente.
     * Se l'utente ha saltato la domanda, il valore è -1.
     */
    private final Map<Question, Integer> domandaRisposte;

    /**
     * Contesto applicativo corrente, che collega controller, utente, e servizi condivisi tra le varie schermate.
     */
    private final AppContext appContext;

    /**
     * Momento in cui è stata visualizzata la prima domanda del quiz.
     * Serve per misurare il tempo impiegato dall'utente nel rispondere a tutte le domande.
     */
    private LocalDateTime questionStartTime;

    /**
     * Indica se la generazione asincrona delle domande è stata completata ed è possibile procedere con il quiz.
     */
    private final BooleanProperty questionsReady;

    /**
     * Indica se è trascorso il tempo minimo richiesto per poter saltare la lettura dei documenti.
     */
    private final BooleanProperty minTimeElapsed;

    /**
     * Numero totale di domande da porre nella sessione di gioco.
     * Impostato in base alla difficoltà o alle impostazioni correnti.
     */
    private int questionCount;

    /**
     * Punteggio assegnato per ogni risposta corretta fornita dal giocatore.
     * Il valore può dipendere dalla difficoltà selezionata.
     */
    private int scorePerQuestion;

    /** Tempo minimo per skippare la lettura dei documenti.
     * <p>
     * Questo valore rappresenta il tempo minimo in secondi che deve trascorrere prima che l'utente possa saltare la lettura dei documenti.
     * Si tiene presente che il pulsante di skipReadingBtn viene abilitato automaticamente solamente quando sono trascorsi i secondi minimi e il thread di analisi ha terminato la generazione delle domande.
     * </p>
     */
    private static final int MIN_TIME_FOR_SKIP = 30;

    /** Tempo limite per rispondere a una domanda del quiz.
     * <p>
     * Questo valore rappresenta il tempo massimo in secondi che l'utente ha per rispondere a ciascuna domanda del quiz.
     * Se il tempo scade, la risposta viene considerata saltata e viene mostrata la risposta corretta.
     * </p>
     */
    private static final java.time.Duration QUESTION_TIME_LIMIT = java.time.Duration.ofSeconds(15) ;

    /**
     * Controller della sessione di gioco per l'applicazione Wordageddon.
     * <p>
     * Gestisce l'interazione dell'utente durante la partita: selezione della difficoltà,
     * lettura dei documenti, quiz a domande multiple e visualizzazione del report finale.
     * Utilizza servizi JavaFX asincroni per mantenere la UI reattiva.
     *
     */
    public GameController(AppContext appContext) {
        this.gameService = appContext.getGameService();
        this.appContext= appContext;
        currentDocumentIndex = new SimpleIntegerProperty(0);
        currentQuestionIndex = new SimpleIntegerProperty(0);
        elapsedSeconds = new SimpleIntegerProperty(0);
        questionsReady = new SimpleBooleanProperty(false);
        minTimeElapsed = new SimpleBooleanProperty(false);
        domandaRisposte = new LinkedHashMap<>();
        score = 0;
        numeroRisposteCorrette = 0;
        numeroRisposteSaltate = 0;
        questionCount = 0;
        scorePerQuestion = 0;
    }

    /**
     * Inizializza la sessione di gioco e configura i servizi asincroni.
     * <p>
     * Questo metodo viene chiamato automaticamente da JavaFX al momento del caricamento del GameController.
     * </p>
     * <ul>
     *   <li>Carica la vista per la selezione della difficoltà</li>
     *   <li>Configura il pulsante di skip durante la lettura dei documenti: viene abilitato automaticamente quando questionsReady è true (ossia le domande sono state generate)
     *       e sono trascorsi almeno 15 secondi dall'inizio del timer (minTimeElapsed)</li>
     *   <li>Istanzia e avvia {@code readingSetupService} per leggere i documenti in modo asincrono e ottenere il testo da mostrare tramite
     *       {@link GameService#setupReadingPhase()}</li>
     *   <li>Istanzia {@code questionSetupService} per generare le domande del quiz in background tramite {@link GameService#getQuestions()}</li>
     *   <li>Aggiorna lo stato questionsReady al completamento della generazione delle domande</li>
     * </ul>
     */
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        GameSessionState sessionState = appContext.getInterruptedSession();
        if (sessionState != null) {
            // Recupera stato da sessione
            restoreSession(sessionState);
            appContext.setInterruptedSession(null);
            new File("interruptedSession.ser").delete();
        } else {

            // IL pulsante di skip viene abilitato automaticamente quando la generazione delle domande è completata e sono trascorsi almeno x dall'inizio del timer
            skipReadingBtn.disableProperty().bind(
                    minTimeElapsed.not().or(questionsReady.not())
            );

            //Instanziazione dei servizi per la generazione asincrona del testo e delle domande

            /*
             * Avvia la fase di lettura dei documenti in un servizio asincrono.
             * Alla conclusione, aggiorna la UI con il testo letto, avvia la generazione delle domande
             * e imposta il timer per la lettura.
             */
            readingSetupServiceFX = new Service<>() {
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

                        // Al termine del task che prepara i documenti alla lettura, parte direttamente la generazione delle domande
                        questionSetupServiceFX.start();
                        java.time.Duration seconds = gameService.getTimeLimit();
                        readingTimer = startTimer(seconds, timerLabelRead, timerBar, () -> loadPane(questionPane));

                        // Avvia la pausa per abilitare lo skip dopo 15 secondi
                        PauseTransition waitSeconds = new PauseTransition(Duration.seconds(MIN_TIME_FOR_SKIP));
                        waitSeconds.setOnFinished(_ -> minTimeElapsed.set(true));
                        waitSeconds.play();
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
            questionSetupServiceFX = new Service<>() {
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
            questionSetupServiceFX.setOnSucceeded(_ -> {
                questions = questionSetupServiceFX.getValue();
                questionsReady.set(true);  // Le domande sono pronte
                questionCount = gameService.getQuestionCount();
                scorePerQuestion = gameService.getScorePerQuestion();
            });
            questionSetupServiceFX.setOnFailed(_ -> {
                throw new RuntimeException("Error during reading setup task: " + questionSetupServiceFX.getException());
            });

            loadPane(diffSelectionPane);
        }
    }

    /**
     * Mostra il contenuto del documento corrente nell'area di lettura.
     *
     * @param i Indice del documento da visualizzare.
     */
    private void setDocument(int i) {
        Document doc = gameService.getDocuments().get(i);
        documentTitleLabel.setText(doc.title());
        textDisplayArea.setText(documentToTextMap.get(doc));
    }

    /**
     * Avvia la fase del quiz dopo la lettura.
     * <p>
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
     * Visualizza una domanda e le sue risposte nella UI.
     * <p>
     * Gestisce la risposta dell'utente, il controllo correttezza,
     * e avanza alla prossima domanda o al report finale se non ci sono piu domande.
     *
     * @param index Indice della domanda da mostrare.
     */
    private void showQuestion(int index) {
        //Se il l'indice della prossima domanda da visualizzare è maggiore del numero di domande, viene chiamato loadPane(reportPane)
        if (currentQuestionIndex.get() >= questions.size()) {
            loadPane(reportPane);
            return;
        }
        // alla prima domanda da mostrare, viene segnato il tempo nella variabile questionStartTime.
        // in generateReport verrà calcolato il tempo che intercorre tra questionStartTime e questionEndTime
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
                    score += scorePerQuestion;
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
     * Salva lo stato corrente della sessione di gioco su disco per consentire all'utente di riprendere la partita successivamente.
     * <p>
     * La sessione viene serializzata in un file (il cui percorso è ottenuto da {@link Config.Props#INTERRUPTED_SESSION_FILE})
     * tramite {@link ObjectOutputStream}. Lo stato scritto consiste in un'istanza di {@link GameSessionState}.
     * <b>Uso tipico:</b> questo metodo viene chiamato quando l'utente interrompe una partita (ad esempio chiudendo l'applicazione mentre é in fase 'quiz')
     * per garantire che tutti i progressi siano salvati e possano essere ripristinati in un secondo momento tramite la funzionalità di "riprendi partita".
     * <p>
     * In caso di errore nella scrittura del file, il metodo logga l'eccezione sia tramite {@link SystemLogger#log} che su standard output.
     *
     * @see GameSessionState
     * @see #restoreSession(GameSessionState)
     */
    public void saveSession() {
        GameSessionState state = new GameSessionState(
                appContext.getCurrentUser(),
                questions,
                domandaRisposte,
                currentQuestionIndex.get(),
                questionStartTime,
                scorePerQuestion,
                gameService.getParams()
                );
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Config.get(Config.Props.INTERRUPTED_SESSION_FILE)))) {
            out.writeObject(state);
        } catch (IOException e) {
            SystemLogger.log("Errore durante il salvataggio della sessione: ", e);
            System.out.println("Errore durante il salvataggio della sessione: " + e.getMessage());
        }
    }

    /**
     * Ripristina lo stato della sessione di gioco da un'istanza di GameSessionState.
     * <p>
     * Viene chiamato quando l'utente decide di riprendere una sessione interrotta.
     * Inizializza il GameService con la difficoltà e i documenti salvati,
     * ripristina le domande e le risposte date, e carica il pannello delle domande.
     * </p>
     *
     * @param state Stato della sessione da ripristinare.
     */
    public void restoreSession(GameSessionState state) {
        System.out.println("Ripristino da sessione interrotta...");
        GameParams params = state.gameParams();
        gameService.restoreParams(params);
        this.questionCount = params.getQuestionCount();
        this.questions = state.questions();
        this.domandaRisposte.clear();
        this.domandaRisposte.putAll(state.domandaRisposte());
        this.currentQuestionIndex.set(state.currentQuestionIndex());
        this.questionStartTime = state.questionStartTime();
        this.scorePerQuestion = state.scorePerQuestion();
        this.score = recalculateScore();
        loadPane(questionPane);
    }

    /**
     * Ricalcola il punteggio totale basato sulle risposte date dall'utente.
     * @return punteggio ricalcolato
     */
    private int recalculateScore() {
        int newScore = 0;
        numeroRisposteCorrette = 0;
        numeroRisposteSaltate = 0;
        for (Map.Entry<Question, Integer> entry : domandaRisposte.entrySet()) {
            int answerIndex = entry.getValue();
            Question q = entry.getKey();
            if (answerIndex == -1) {
                numeroRisposteSaltate++;
            } else if (answerIndex == q.correctAnswerIndex()) {
                System.out.println("Risposta corretta per la domanda: " + q.text());
                newScore += scorePerQuestion; // Usa il valore ripristinato!
                numeroRisposteCorrette++;
            }
        }
        System.out.println("NewScore: " + newScore);
        return newScore;
    }


    /**
     * Genera il report di fine partita e aggiorna la UI.
     * <p>
     * Calcola il tempo impiegato per rispondere alle domande, crea e salva il 'GameReport' registrato, aggiorna le statistiche della nuova vista (score,
     * risposte corrette, risposte sbagliate, percentuale di risposte esatte su quelle date)
     * e popola la tabella che mostra il resoconto della partita tramite il metodo populateAnswerTable() del controller.
     *
     * @see GameController#populateAnswerTable()
     */
    private void generateReport() {
        // Se il report viene generato, la session é conclusa quindi cancello il file
        File file = new File(Config.get(Config.Props.INTERRUPTED_SESSION_FILE));
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                System.out.println("Errore: impossibile cancellare il file di sessione!");
            }
        }
        LocalDateTime questionEndTime = LocalDateTime.now();
        java.time.Duration usedTime = java.time.Duration.between(questionStartTime, questionEndTime);
        java.time.Duration timeLimit = QUESTION_TIME_LIMIT.multipliedBy(questionCount);

        GameReport report = new GameReport(
                appContext.getCurrentUser(),
                gameService.getDocuments(),
                LocalDateTime.now(),
                gameService.getDifficulty(),
                timeLimit,
                usedTime,
                questionCount,
                score
        );
        gameService.saveGameReport(report);
        scoreValue.setText(String.valueOf(score));
        rightValue.setText(String.valueOf(numeroRisposteCorrette));
        wrongValue.setText(String.valueOf(questionCount - numeroRisposteCorrette));
        questionNumber.setText(String.valueOf(questionCount));

        int numeroRisposteDate = questionCount - numeroRisposteSaltate;
        double percentualeCompletamento = (double) numeroRisposteDate / questionCount  * 100;

        completionValue.setText(String.format("%.2f%%", percentualeCompletamento));
        populateAnswerTable();
    }

    /**
     * Avvia un timer countdown con aggiornamento su barra e label.
     * <p>
     * Mostra il tempo residuo, aggiorna il colore della barra in base al tempo rimasto
     * e chiama una funzione al termine del countdown.
     * </p>
     *
     * @param duration Durata totale del timer.
     * @param label Label da aggiornare con il tempo rimanente.
     * @param bar ProgressBar da aggiornare.
     * @param onFinished Callback da eseguire al termine del timer.
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
     * Cambia il pannello visibile nello StackPane principale.
     * <p>
     * Esegue azioni specifiche a seconda del pannello mostrato (es. avvio lettura, avvia domande, mostra risultati).
     * </p>
     *
     * @param pane Nodo FXML da rendere visibile.
     */
    private void loadPane(Node pane) {
        for (Node p : stackPane.getChildren()) {
            p.setVisible(false);
        }
        switch (pane.getId()) {
            case "readingPane" -> readingSetupServiceFX.start();
            case "questionPane" -> switchToQuestions();
            case "reportPane" -> generateReport();
            default -> {
            }

        }
        pane.setVisible(true);
    }
    /**
     * Ritorna l'ID del pannello attualmente visibile nello StackPane.
     * <p>
     * Scorre i nodi figli dello StackPane e restituisce l'ID del primo nodo visibile.
     * </p>
     *
     * @return ID del pannello visibile, o null se nessun pannello è visibile.
     */
    public String getCurrentPaneId() {
        for (Node node : stackPane.getChildren()) {
            if (node.isVisible()) {
                return node.getId();
            }
        }
        return null;
    }

    /**
     * Gestisce la selezione della difficoltà da parte dell'utente.
     * <p>
     * Metodo chiamato quando l'utente seleziona un livello di difficoltà tramite pulsante.
     * In base all'ID del pulsante cliccato inizializza il GameService con il livello di difficoltà corrispondente.
     * Avvia la fase di lettura dei documenti caricando il relativo pannello ('readingPane').
     * </p>
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
     * Ritorna alla schermata principale del menu.
     */
    @FXML
    private void onBackPressed() {
        //Torna al menu principale o chiudi la finestra
        ViewLoader.load(ViewLoader.View.MENU);
    }

    /**
     * Cambia il documento visualizzato nella fase di lettura.
     *
     * @param event Evento generato dai pulsanti "Successivo" o "Precedente".
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
     * Salta la lettura dei documenti e passa direttamente alle domande.
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
     */
    @FXML
    public void handleShowLeaderboard() {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }


    /**
     * Alterna la visualizzazione della sezione dei Report e della TableView popolata tramite il metodo {@code populateAnswerTable}.
     *
     */
    @FXML
    public void toggleShowAnswers() {
        heroBox.setVisible(!heroBox.isVisible());
        viewAnswersBtnText.setText((heroBox.isVisible() ? "Mostra Risposte" : "Mostra Resoconto" ));
        answersBox.setVisible(!answersBox.isVisible());



    }
    /**
     * Gestisce il click sul pulsante "Menu" del presente nella pagina dei Risultati.
     * Carica la schermata del Menu utilizzando il {@code ViewLoader}.
     *
     */
    @FXML
    public void handleGoMenu() {
        ViewLoader.load(ViewLoader.View.MENU);
    }
    /**
     * Gestisce il click sul pulsante "Play Again" del presente nella pagina dei Risultati.
     * Carica la schermata del Game utilizzando il {@code ViewLoader}.
     *
     */
    @FXML
    public void handlePlayAgain() {
        ViewLoader.load(ViewLoader.View.GAME);
    }

    /**
     *Popola la tabella delle risposte visibile a fine partita.
     * Il metodo viene chiamato in {@code generateReport()} per visualizzare il riepilogo
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

        punteggioCln.setCellFactory(_ -> new TableCell<>() {
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
