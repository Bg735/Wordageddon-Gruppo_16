package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.services.GameService;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.GameService.Question;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

    public class GameSessionController {

        @FXML private AnchorPane readingPane;
        @FXML private AnchorPane questionPane;

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

        public GameSessionController(AppContext appContext) {
            this.gameService = appContext.getGameService();
        this.questions = new ArrayList<>();

        }
        @FXML
        public void initialize() {
            readingPane.setVisible(true);
            questionPane.setVisible(false);

            setupReadingPhase();
            generateQuestionsInBackground();
        }

        private void setupReadingPhase() {
            //mostra i documenti
            StringBuilder text = new StringBuilder();
            for (Document doc : gameService.getDocuments()) {
                Path path = Path.of(doc.path());
                try {
                    text.append(Files.readString(path)).append("\n\n");
                } catch (IOException e) {
                    SystemLogger.log("Errore nella lettura del documento", e);
                }
            }
            textDisplayArea.setText(text.toString());

            //avvia timer lettura
            int seconds = (int) gameService.getTimeLimit().getSeconds();
            Timeline readTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int remaining = Integer.parseInt(timerLabelRead.getText().replace("s", "")) - 1;
                timerLabelRead.setText(remaining + "s");
                timerBar.setProgress((double)(seconds - remaining) / seconds);
                if (remaining <= 0) {
                    ((Timeline)e.getSource()).stop();
                    switchToQuestions();
                }
            }));
            timerLabelRead.setText(seconds + "s");
            timerBar.setProgress(0);
            readTimer.setCycleCount(seconds);
            readTimer.play();
        }

        private void generateQuestionsInBackground() {
            Task<List<Question>> task = new Task<>() {
                @Override
                protected List<Question> call() {
                    return gameService.getQuestions();
                }
            };
            task.setOnSucceeded(e -> questions = task.getValue());
            task.setOnFailed(e -> SystemLogger.log("Errore generazione domande", task.getException()));
            new Thread(task).start();
        }

        private void switchToQuestions() {
            if (questions.isEmpty()) {
                PauseTransition wait = new PauseTransition(Duration.seconds(1));
                wait.setOnFinished(e -> switchToQuestions());
                wait.play();
                return;
            }
            readingPane.setVisible(false);
            questionPane.setVisible(true);
            showQuestion(currentQuestionIndex);
        }

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

            setupQuestionTimer();
        }

        private void handleAnswer(int selectedIndex) {
            // TODO: salva la risposta e passa alla prossima domanda
            questionTimer.stop();
            currentQuestionIndex++;
            showQuestion(currentQuestionIndex);
        }

        private void setupQuestionTimer() {
            int questionTime = 20;
            timerLabelQuestion.setText(questionTime + "s");
            timerBarQuestion.setProgress(0);
            questionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                int remaining = Integer.parseInt(timerLabelQuestion.getText().replace("s", "")) - 1;
                timerLabelQuestion.setText(remaining + "s");
                timerBarQuestion.setProgress((double)(20 - remaining) / 20);
                if (remaining <= 0) {
                    questionTimer.stop();
                    currentQuestionIndex++;
                    showQuestion(currentQuestionIndex);
                }
            }));
            questionTimer.setCycleCount(questionTime);
            questionTimer.play();
        }

        private void endGame() {
            // TODO: salva le risposte, mostra risultato e ritorna al menu
            System.out.println("Partita terminata");
        }
    }

