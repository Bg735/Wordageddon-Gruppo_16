package it.unisa.diem.wordageddon_g16.controllers;


import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
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
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Gestione Admin");
            popupStage.setHeight(400);
            popupStage.setWidth(300);
            popupStage.setResizable(false);

            VBox root = new VBox(15);
            root.setPadding(new Insets(10));
            root.setSpacing(10);
            List<User> otherUsers=service.getAllUsersExceptCurrent();
            if (otherUsers.isEmpty()) {
                Label noUsersLabel = new Label("Nessun altro utente disponibile.");
                root.getChildren().add(noUsersLabel);
            } else {
                for (User user : otherUsers ){
                    HBox userRow = new HBox(20);
                    userRow.setAlignment(Pos.CENTER_LEFT);
                    Label nameLabel = new Label(user.getName());
                    nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                    ToggleButton toggle = new ToggleButton();
                    toggle.setText(user.isAdmin() ? "Admin" : "User");
                    toggle.setSelected(user.isAdmin());

                    toggle.setOnAction(e -> {
                        boolean nowAdmin = toggle.isSelected();
                        toggle.setText(nowAdmin ? "Admin" : "User");

                        if (nowAdmin) {
                            service.promoteUser(user.getName());
                        } else {
                            service.demoteUser(user.getName());
                        }
                    });

                    userRow.getChildren().addAll(nameLabel, toggle);
                    root.getChildren().add(userRow);
                }

            }
        Scene scene = new Scene(root);
        //scene.getStylesheets().add(getClass().getResource("/style/popup.css").toExternalForm());
        scene.getStylesheets().add(Resources.getStyle("popup"));

        popupStage.setScene(scene);
        popupStage.showAndWait();

    }

    @FXML
    void handleDocumenti(ActionEvent event) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Gestione Documenti");
        popup.setHeight(400);
        popup.setWidth(300);
        popup.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        //ottengo i doc dal db
        List<Document> documenti = DocumentDAO.selectAll();
        if(documenti.isEmpty()) {
            root.getChildren().add(new Label("Non ci sono documenti disponibili!"));
        }
        else {
            for(Document d : documenti) {
                HBox docRow = new HBox(15);
                docRow.setAlignment(Pos.CENTER_LEFT);
                Label lab = new Label(d.getTitle() + "(ID:" + d.getId() +  "," + d.getWordCount() + "parole )");
                Button bot = new Button("delete");
                Button bot1 = new Button("Open doc");
                bot.setOnAction(e -> {
                    DocumentDAO.delete(d);
                    root.getChildren().remove(docRow);

                });
                bot1.setOnAction(e1 -> {
                    try{
                        Desktop.getDesktop().open(new File(d.getPath()));
                    }catch(IOException ex){
                        System.out.println("error opening doc");
                    }
                });

                docRow.getChildren().addAll(lab, bot);
                docRow.getChildren().addAll(bot1);
                root.getChildren().add(docRow);

            }
        }
     Scene scene = new Scene(root);
        scene.getStylesheets().add(Resources.getStyle("popup"));
        popup.setScene(scene);
        popup.showAndWait();

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
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        //permetto di aggiungere una stopword
        TextField wordInput = new TextField();
        wordInput.setPromptText("Inserisci una nuova stopword");

        //le inserisco in una lista di sw
        ListView<String> sw = new ListView<>();
        sw.getItems().addAll(service.getAllStopwords());

        //aggiunta manuale sw
        Button btnAdd = new Button("Aggiungi");
        btnAdd.setOnAction(e -> {
            String word = wordInput.getText().trim().toLowerCase();
            if (!word.isEmpty() && !sw.getItems().contains(word)) {
                service.addStopWords(word);
                sw.getItems().add(word);
                wordInput.clear();
            } else {
                System.out.println("StopWord vuota o già presente!");
            }
        });

        //carico file
        Button btnFile = new Button("Carica da file");
        btnFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleziona file di stopwords");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showOpenDialog(popupStage);
            if (file != null) {
                try {
                    service.addStopwordsFromFile(file);
                    sw.getItems().setAll(service.getAllStopwords());
                } catch (RuntimeException ex) {
                    System.out.println("Errore di stopwords");
                    ex.printStackTrace();
                } catch (IOException ex) {
                    System.out.println("Errore nella chiusura del file di testo");
                    throw new RuntimeException(ex);
                }
            }
        });
        Button removeButton = new Button("Rimuovi selezionata");
        removeButton.setOnAction(e -> {
            String selected = sw.getSelectionModel().getSelectedItem();
            if (selected != null) {
                service.deleteStopword(selected);
                sw.getItems().remove(selected);
            }
        });
        root.getChildren().addAll(
                new Label("Inserisci una nuova stopword:"),
                wordInput,
                btnAdd,
                new Label("Oppure caricala da file:"),
                btnFile,
                new Label("StopWords attuali:"),
                sw, removeButton );
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