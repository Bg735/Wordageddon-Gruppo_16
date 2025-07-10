package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @class GameSessionController
 * @brief Gestisce la sessione di gioco, mostrando il documento da leggere e le domande successive.
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

    @FXML private Label questionText;
    @FXML private VBox answerBox;
    @FXML private Label questionCountLabel;
    @FXML private ProgressBar timerBarQuestion;
    @FXML private Label timerLabelQuestion;
    @FXML private Button nextButton;

    private final GameService gameService;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Timeline questionTimer;

    /**
     * @brief Costruttore.
     * @param appContext Contesto dell'applicazione che fornisce il GameService.
     */
    public GameSessionController(AppContext appContext) {
        this.gameService = appContext.getGameService();
        this.questions = gameService.getQuestions();
    }

    /**
     * @brief Metodo chiamato automaticamente all'inizializzazione del controller.
     * Rende visibile inizialmente lo StackPane che si occupa della visualizzazione del testo da leggere con il timer.
     */
    @FXML
    public void initialize() {
        loadPane(diffSelectionPane);
        //generateQuestionsAsync();
    }

    /**
     * @brief Avvia in background la generazione delle domande per la sessione di gioco.
     *
     * Questo metodo crea un Service JavaFX che lancia in modo asincrono un Task
     * per la generazione delle domande, sfruttando il metodo getQuestions() del GameService.
     *
     * La generazione avviene durante la fase di lettura dei documenti (readingPane), così da non bloccare l'interfaccia utente.
     *
     * Al completamento con successo, la lista delle domande generate viene salvata in questions.
     *
     * @note Deve essere invocato prima della visualizzazione delle domande (prima di `switchToQuestions()`).
     */

   /* private void generateQuestionsAsync() {
        Service<List<Question>> service = new Service<>() {
            @Override
            protected Task<List<Question>> createTask() {
                return new GenerateQuestionsTask(gameService);
            }
        };
        service.setOnSucceeded(e -> questions = service.getValue());
        service.setOnFailed(e -> SystemLogger.log("Errore generazione domande", service.getException()));
        service.start();
    }
*/
    /**
     * @brief Mostra il testo dei documenti e avvia il timer della fase di lettura.
     */
    private void setupReadingPhase() {
        //Viene unito in un unico testo tutti i documenti generati in base alla difficoltà
        // dal metodo generateDocuments(float influence) del GameService
        StringBuilder text = new StringBuilder();
        for (Document doc : gameService.getDocuments()) {
            Path path = doc.path();
            try {
                text.append(Files.readString(path)).append("\n\n");
            } catch (IOException e) {
                SystemLogger.log("Errore nella lettura del documento", e);
            }
        }
        textDisplayArea.setText(text.toString());

        // Si trasforma il timer calcolato dal metodo generateTimer(float influence) del GameService in secondi
        int seconds = (int) gameService.getTimeLimit().getSeconds();
        startTimer(seconds, timerLabelRead, timerBar, ()->loadPane(questionPane));
    }


    /**
     * @brief Passa dalla fase di lettura a quella delle domande:
     *
     */
    private void switchToQuestions() {
        if (questions.isEmpty()) {
            PauseTransition wait = new PauseTransition(Duration.seconds(1));
            wait.setOnFinished(e -> switchToQuestions());
            wait.play();
            return;
        }
        showQuestion(currentQuestionIndex);
    }

    /**
     * @brief Mostra la domanda corrente e prepara i pulsanti delle risposte.
     * @param index Indice della domanda corrente.
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
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
        });


    }

    /**
     * @brief Gestisce la selezione della risposta da parte dell’utente.
     * @param selectedIndex Indice della risposta scelta.
     */
    private void handleAnswer(int selectedIndex) {

    }


    /**
     * @brief Metodo chiamato al termine della sessione di gioco.
     */
    private void endGame() {
    }

    /**
     * @brief Avvia un timer con aggiornamento visivo e callback finale.
     *
     * Questo metodo crea e avvia un timer che decrementa ogni secondo, aggiornando:
     * - la label del tempo residuo (`label`)
     * - la barra di avanzamento (`bar`)
     *
     * Al termine del timer (quando il tempo arriva a 0), viene eseguito il codice passato tramite `onFinished`.
     *
     * @param durationSeconds Durata del timer in secondi.
     * @param label Etichetta testuale da aggiornare con il tempo rimanente (es. "59s").
     * @param bar Progress bar da aggiornare visivamente in base al tempo trascorso.
     * @param onFinished Operazione da eseguire automaticamente al termine del timer.
     *
     * @return Timeline oggetto che rappresenta il timer in esecuzione.
     */
    private Timeline startTimer(int durationSeconds, Label label, ProgressBar bar, Runnable onFinished) {
        //Imposta la label che mostra il tempo rimanente a durationSeconds
        label.setText(durationSeconds + "s");

        bar.setProgress(0);

        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            //Viene descrementato il tempo rimanente
            int remaining = Integer.parseInt(label.getText().replace("s", "")) - 1;
            label.setText(remaining + "s");
            bar.setProgress((double) (durationSeconds - remaining) / durationSeconds);
            if (remaining <= 0) {
                ((Timeline) e.getSource()).stop(); //Blocca la Timeline
                onFinished.run(); // Viene eseguito il metodo passato
            }
        }));
        timer.setCycleCount(durationSeconds);
        timer.play();
        return timer;
    }

    private void loadPane(Node pane) {
        for(Node p : stackPane.getChildren()) {
            p.setVisible(false);
        }
        switch(pane.getId()){
            case "readingPane" -> setupReadingPhase();
            case "questionPane" -> switchToQuestions();
            default -> {}
        }
        pane.setVisible(true);
    }

    public void onDifficultySelected(ActionEvent event) {
        switch (((Button) event.getSource()).getId()){
            case "diffEasyBTN" -> gameService.init(Difficulty.EASY);
            case "diffMediumBTN" -> gameService.init(Difficulty.MEDIUM);
            case "diffHardBTN" -> gameService.init(Difficulty.HARD);
            default -> throw new IllegalArgumentException("Difficoltà non riconosciuta");
        }

    }
}