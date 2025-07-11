package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
//import it.unisa.diem.wordageddon_g16.services.tasks.GenerateQuestionsTask;



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
    @FXML private Button answer1Btn;
    @FXML private Button answer2Btn;
    @FXML private Button answer3Btn;
    @FXML private Button answer4Btn;
    @FXML private Button nextButton;
    @FXML private Button backButton;

    private final GameService gameService;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Timeline questionTimer;

    private Service<StringBuffer> readingSetupService;
    private Service questionSetupService;


    /**
     * @brief Costruttore.
     * @param appContext Contesto dell'applicazione che fornisce il GameService.
     */
    public GameSessionController(AppContext appContext) {
        this.gameService = appContext.getGameService();

    }

    /**
     * @brief Metodo chiamato automaticamente all'inizializzazione del controller.
     * Rende visibile inizialmente lo StackPane che si occupa della visualizzazione del testo da leggere con il timer.
     */
    @FXML
    public void initialize() {
        readingSetupService = new Service<>() {
            @Override
            protected Task<StringBuffer> createTask() {
                Task<StringBuffer> task= new Task<>() {
                    @Override
                    protected StringBuffer call() {
                        return gameService.setupReadingPhase();
                    }
                };
                task.setOnSucceeded(_ -> {
                    StringBuffer text = task.getValue();
                    Platform.runLater(
                        () -> {
                            textDisplayArea.setText(text.toString());
                        }
                    );

                    int seconds = (int) gameService.getTimeLimit().getSeconds();
                    // alla fine del timer mostra questionPane
                    startTimer(3, timerLabelRead, timerBar, () -> loadPane(questionPane)); //METTI SECOND


                    questionSetupService = new Service<List<Question>>() {
                        @Override
                        protected Task<List<Question>> createTask() {
                            return new Task<>() {
                                @Override
                                protected List<Question> call() {
                                    return gameService.getQuestions(); // recupera le domande
                                }
                            };
                        }
                    };
                    questionSetupService.setOnSucceeded(e -> {
                        questions = (List<Question>) questionSetupService.getValue();
                    });
                    questionSetupService.setOnFailed(_ -> endGame());
                    questionSetupService.start();


                });
                task.setOnFailed(_ -> endGame());
                return task;
            }
        };

        loadPane(diffSelectionPane);
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

        List<String> answers = q.answers();
        Button[] buttons = { answer1Btn, answer2Btn, answer3Btn, answer4Btn };

        for (int i = 0; i < buttons.length; i++) {
            Button btn = buttons[i];
            if (i < answers.size()) {
                btn.setText(answers.get(i));
                btn.setDisable(false);
                btn.setStyle("");
                btn.setVisible(true);

                final int answerIndex = i;
                btn.setOnAction(e -> {
                    // ✅ STOPPA IL TIMER
                    if (questionTimer != null) {
                        questionTimer.stop();
                        questionTimer = null;
                    }

                    // Controlla la risposta
                    boolean isCorrect = answerIndex == q.correctAnswerIndex();

                    if (isCorrect) {
                        btn.setStyle("-fx-background-color: #4CAF50;");
                    } else {
                        btn.setStyle("-fx-background-color: #F44336;");
                        buttons[q.correctAnswerIndex()].setStyle("-fx-background-color: #4CAF50;");
                    }

                    // Disabilita tutti i bottoni
                    for (Button b : buttons) {
                        b.setDisable(true);
                    }

                    // ✅ Pausa di 0.5 secondi per far vedere la risposta corretta
                    PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                    pause.setOnFinished(ev -> {
                        currentQuestionIndex++;
                        showQuestion(currentQuestionIndex);
                    });
                    pause.play();
                });
            } else {
                btn.setVisible(false);
            }
        }

        questionTimer = startTimer(20, timerLabelQuestion, timerBarQuestion, () -> {
            // Tempo scaduto → mostra la risposta corretta e vai avanti dopo 0.5s
            Platform.runLater(() -> {
                // Disabilita bottoni
                for (Button b : buttons) {
                    b.setDisable(true);
                }

                // Evidenzia la risposta corretta
                buttons[q.correctAnswerIndex()].setStyle("-fx-background-color: #4CAF50;");

                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> {
                    currentQuestionIndex++;
                    showQuestion(currentQuestionIndex);
                });
                pause.play();
            });
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
        System.out.println("Game over");
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
        label.setText(durationSeconds + "s");
        bar.setProgress(0);

        final int[] remainingTime = {durationSeconds};
        Timeline[] timelineRef = new Timeline[1]; // workaround per accedere dall’interno

        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingTime[0]--;
            label.setText(remainingTime[0] + "s");
            bar.setProgress((double) (durationSeconds - remainingTime[0]) / durationSeconds);

            if (remainingTime[0] <= 0) {
                timelineRef[0].stop();
                onFinished.run();
            }
        }));

        timelineRef[0] = timer; // assegna la variabile finale
        timer.setCycleCount(durationSeconds);
        timer.play();
        return timer;
    }

    private void loadPane(Node pane) {
        for(Node p : stackPane.getChildren()) {
            p.setVisible(false);
        }
        switch(pane.getId()){
            case "readingPane" -> readingSetupService.start();
            case "questionPane" -> showQuestion(0);
            default -> {}
        }
        pane.setVisible(true);
    }

    @FXML
    public void onDifficultySelected(ActionEvent event) {
        switch (((Button) event.getSource()).getId()){
            case "diffEasyBTN" -> gameService.init(Difficulty.EASY);
            case "diffMediumBTN" -> gameService.init(Difficulty.MEDIUM);
            case "diffHardBTN" -> gameService.init(Difficulty.HARD);
            default -> throw new IllegalArgumentException("Difficoltà non riconosciuta");
        }
        loadPane(readingPane);
    }

    @FXML
    private void onBackPressed(ActionEvent event) {
        //Torna al menu principale o chiudi la finestra
        ViewLoader.load(ViewLoader.View.MENU);

    }
}