package it.unisa.diem.wordageddon_g16.controllers;


import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.JdbcDAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.JdbcRepository;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.awt.SystemColor.window;

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
        JdbcRepository repo = new JdbcRepository();
        try {
            UserDAO userDAO = repo.getDAO("user");
            List<User> users = userDAO.selectAll();
            users.remove(currentUser);
            VBox userListVBox = new VBox(10);
            userListVBox.setPadding(new Insets(10));
            userListVBox.setSpacing(10);

            for (User user : users) {
                HBox userRow = new HBox(20);
                userRow.setAlignment(Pos.CENTER_LEFT);
                Label nameLabel = new Label(user.getName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                Button btnUser = new Button(user.isAdmin() ? "Admin" : "User");
                btnUser.getStyleClass().add("Btn");

                //inverto stato e di conseguenza aggiorno il db
                btnUser.setOnAction(e -> {
                    user.setAdmin(!user.isAdmin());
                    userDAO.update(user);
                    btnUser.setText(user.isAdmin() ? "Admin" : "User");
                });

                userRow.getChildren().addAll(nameLabel, btnUser);
                userListVBox.getChildren().add(userRow);
            }
            ScrollPane scrollPane = new ScrollPane(userListVBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox content = new VBox(10, new Label("Gestione privilegi amministratore:"), scrollPane);
            content.setPadding(new Insets(20));

            Popup popup = new Popup();
            popup.getContent().add(content);
            Node source = (Node) event.getSource();
            Window window = source.getScene().getWindow();
            popup.show(window);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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