package it.unisa.diem.wordageddon_g16.controllers;


import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    @FXML private AnchorPane anchorSemicerchio;

    @FXML private StackPane stackMedio;

    private final User currentUser;
    private final AppContext appContext;

    public UserPanelController(AppContext context) {
        this.service = context.getUserPanelService();
        currentUser=context.getCurrentUser();
        this.appContext = context;
    }

    @FXML
    void handleAdmin(ActionEvent event) {


    }

    @FXML
    void handleDocumenti(ActionEvent event) {

    }

    @FXML
     void handleLogOut(ActionEvent event) {
        appContext.getAuthService().logout();
        ViewLoader.load(ViewLoader.View.AUTH);
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.MENU);
    }

    @FXML
    void handleStopWords(ActionEvent event) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Gestione Stopwords");
        popupStage.setHeight(400);
        popupStage.setWidth(300);
        popupStage.setResizable(false);

        VBox root = new VBox(10);

        TextField wordInput = new TextField();
        wordInput.setPromptText("Inserisci una nuova stopword");
        Button addButton = new Button("Aggiungi");
        addButton.setOnAction(e -> {
            String word = wordInput.getText().trim();
            if (!word.isEmpty()) {
                service.addStopWords(word);
                wordInput.clear();
            }
        });
        Button uploadButton = new Button("Carica da file");
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona file di stopwords");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(popupStage);
            if (file != null) {
                try {
                    service.addStopwordsFromFile(file);
                } catch (RuntimeException ex) {
                    SystemLogger.log("Errore di stopwords", ex);
                    ex.printStackTrace();
                } catch (IOException ex) {
                    SystemLogger.log("Errore nella chiusura del file di testo", ex);
                    throw new RuntimeException(ex);
                }
            }
        });
        root.getChildren().addAll(
                new Label("Inserisci una nuova stopword:"),
                wordInput,
                addButton,
                new Label("Oppure caricala da file:"),
                uploadButton);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Resources.getStyle("popup"));
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    @FXML
    public void initialize() {
        // Posiziona il secondo StackPane a metà altezza dell'AnchorPane a forma di semicerchio
        anchorSemicerchio.heightProperty().addListener((obs, oldVal, newVal) -> {
            double centerY = (newVal.doubleValue() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });
        Platform.runLater(() -> {
            double centerY = (anchorSemicerchio.getHeight() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });


        var isAdmin= currentUser.isAdmin();
        if(!isAdmin){
            adminPanel.setVisible(false);
        }

        usernameLabel.setText(currentUser.getName());

        livelloClm.setCellValueFactory(report -> new SimpleStringProperty(report.getValue().getDifficulty().toString()));
        punteggioClm.setCellValueFactory(new PropertyValueFactory<>("score"));
        tempoClm.setCellValueFactory(report -> {
            Duration dur = report.getValue().getUsedTime();
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

}