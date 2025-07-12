package it.unisa.diem.wordageddon_g16.controllers;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.event.Event;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;

public class ReportController {
    @FXML
    private StackPane playAgainBtn;
    @FXML
    private StackPane leaderboardBtn;
    @FXML
    private Label questionValue;
    @FXML
    private StackPane showAnswersBtn;
    @FXML
    private VBox answersBox;
    @FXML
    private StackPane mainStack;
    @FXML
    private TableView answersTable;
    @FXML
    private Label rightValue;
    @FXML
    private StackPane menuBtn;
    @FXML
    private HBox actionBarBox;
    @FXML
    private Label wrongValue;
    @FXML
    private Label completionValue;
    @FXML
    private Text scoreValue;
    @FXML
    private VBox heroBox;
    @FXML
    private Label viewAnswersBtnText;

    //private final ResultsService resultsService;
    private final AppContext context;

    public ReportController(AppContext context) {
        this.context = context;
    }

    @FXML
    void handlePlayAgain(MouseEvent event) {
        ViewLoader.load(ViewLoader.View.GAME);
    }

    @FXML
    void toggleShowAnswers(MouseEvent event) {
        answersBox.setVisible(!answersBox.isVisible());
        heroBox.setVisible(!heroBox.isVisible());

        if(heroBox.isVisible()) {
            viewAnswersBtnText.setText("Visualizza Risposte");
        }
        else{
            viewAnswersBtnText.setText("Visualizza Resoconto");
        }
    }

    @FXML
    void handleShowLeaderboard(MouseEvent event) {
        ViewLoader.load(ViewLoader.View.LEADERBOARD);
    }

    @FXML
    public void handleGoMenu(Event event) {
        ViewLoader.load(ViewLoader.View.MENU);
    }

    @FXML
    void initialize() {
        heroBox.setVisible(true);
        answersBox.setVisible(false);
    }
}