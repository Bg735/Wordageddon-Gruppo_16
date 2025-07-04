package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
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

    public UserPanelController(UserPanelService userPanelService) {
        this.service=userPanelService;
        currentUser=service.getAppContext().getCurrentUser();
    }

    @FXML
    void handleAdmin(ActionEvent event) {

    }
//deve apparire un popup per visualizzare i documenti (mettere un bottone con il cestino) quando clicco elimino il documento dal db dei documenti
    @FXML
    void handleDocumenti(ActionEvent event) {

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
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20;");
        //Inserimento manuale
        TextField wordInput = new TextField();
        wordInput.setPromptText("Inserisci una nuova stopword");
        Button addButton = new Button("Aggiungi");
        addButton.setOnAction(e -> {
            String word = wordInput.getText().trim();
            if (!word.isEmpty()) {
                service.addStopword(word);
                wordInput.clear();
            }
        });
        //Upload da file
        Button uploadButton = new Button("Carica da file");
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona file di stopwords");
            File file = fileChooser.showOpenDialog(popupStage);
            if (file != null) {
                try {
                    service.addStopWords(file);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }
        });
        root.getChildren().addAll(new Label("Inserisci stopword manualmente:"), wordInput, addButton,
                new Separator(), new Label("Oppure caricale da file:"), uploadButton);
        Scene scene = new Scene(root);
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