package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.db.DAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

public class UserPanelController {

    @FXML
    private TableColumn<?, ?> livelloCol;

    @FXML
    private Label numPartiteLabel;

    @FXML
    private Label ptMedioLabel;

    @FXML
    private Label ptMiglioreLabel;

    @FXML
    private TableColumn<?, ?> punteggioClm;

    @FXML
    private TableColumn<?, ?> tempoClm;

    @FXML
    private TableView<?> userTableView;

    @FXML
    private Label usernameLabel;

    @FXML
    void handleAdmin(ActionEvent event) {

    }

    @FXML
    void handleDocumenti(ActionEvent event) {

    }

    @FXML
    void handleGoBack(ActionEvent event) {

    }

    @FXML
    void handleStopWords(ActionEvent event) {

    }

    @FXML private AnchorPane anchorSemicerchio;
    @FXML private StackPane stackMedio;

    @FXML
    public void initialize() {
        // Posiziona il secondo StackPane a metà altezza dell'AnchorPane
        anchorSemicerchio.heightProperty().addListener((obs, oldVal, newVal) -> {
            double centerY = (newVal.doubleValue() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });
        Platform.runLater(() -> {
            double centerY = (anchorSemicerchio.getHeight() - stackMedio.getHeight()) / 2;
            stackMedio.setLayoutY(centerY);
        });

    }

    public UserPanelController() {
    }

    /*
    DEVE ESSERE DECOMMENTATO E ELIMINARE L'ALTRO COSTRUTTORE
     public UserPanelController(DAO<Object> user, DAO<Object> gameReport) {
    }
     */

}
