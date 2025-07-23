package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.services.LeaderboardService;
import it.unisa.diem.wordageddon_g16.utility.ViewLoader;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;
/**
 * Controller responsabile della gestione e visualizzazione della schermata di classifica.
 * <p>
 * Carica dinamicamente le classifiche globali e filtrate per difficoltà nella rispettiva {@link TableView}.
 * Utilizza {@link LeaderboardService} per ottenere i dati di gioco e {@link ViewLoader} per la navigazione.
 */
public class LeaderboardController implements Initializable {

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> globalTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> easyTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> mediumTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> hardTW;

    /**
     * Servizio di classifica utilizzato per ottenere i dati delle classifiche.
     */
    private final LeaderboardService leaderboardService;

    /**
     * Costruttore del controller utilizzando il {@link AppContext} per recuperare il {@link LeaderboardService}.
     *
     * @param context contesto applicativo condiviso con il servizio di classifica
     */
    public LeaderboardController(AppContext context) {
        this.leaderboardService = context.getLeaderboardService();
    }
    /**
     * Inizializza la schermata di classifica e popola le tabelle con i dati ottenuti da {@link LeaderboardService}.
     *
     * Per ogni {@link TableView} (globale e per difficoltà):
     * <ul>
     *   <li>Configura la colonna dell'indice con un {@link TableCell} personalizzato</li>
     *   <li>Collega le colonne ai campi di {@link LeaderboardService.LeaderboardEntry}</li>
     *   <li>Popola i dati con {@code FXCollections.observableList(...)} per la difficoltà corrispondente</li>
     * </ul>
     *
     *
     * @param url non utilizzato
     * @param resourceBundle non utilizzato
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Effettuo la stessa configurazione per tutte le TableView
        for (TableView<LeaderboardService.LeaderboardEntry> tableView : new TableView[]{globalTW, easyTW, mediumTW, hardTW}) {
            tableView.setPlaceholder(new javafx.scene.control.Label("Nessun dato disponibile"));

            var indexCol = (TableColumn<LeaderboardService.LeaderboardEntry, Integer>) tableView.getColumns().getFirst();
            indexCol.setCellFactory(_ -> new TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                    } else {
                        setText(String.valueOf(getIndex() + 1));
                    }
                }
            });

            int i=1;
            ((TableColumn<LeaderboardService.LeaderboardEntry, String>) tableView.getColumns().get(i++)).setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));

            if (tableView==globalTW)
                ((TableColumn<LeaderboardService.LeaderboardEntry, String>) tableView.getColumns().get(i++)).setCellValueFactory(data -> {
                    var entry = data.getValue();
                    return new SimpleStringProperty(entry.favouriteDifficulty() == null ? "N/A" : entry.favouriteDifficulty().name());
                });

            ((TableColumn<LeaderboardService.LeaderboardEntry, Integer>) tableView.getColumns().get(i++)).setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().averageScore()).asObject());

            ((TableColumn<LeaderboardService.LeaderboardEntry, Integer>) tableView.getColumns().get(i++)).setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().totalScore()).asObject());

            ((TableColumn<LeaderboardService.LeaderboardEntry, Integer>) tableView.getColumns().get(i)).setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().gamesPlayed()).asObject());

        }
        globalTW.setItems(FXCollections.observableList(leaderboardService.getGloablLeaderboard()));
        easyTW.setItems(FXCollections.observableList(leaderboardService.getLeaderboardByDifficulty(Difficulty.EASY)));
        mediumTW.setItems(FXCollections.observableList(leaderboardService.getLeaderboardByDifficulty(Difficulty.MEDIUM)));
        hardTW.setItems(FXCollections.observableList(leaderboardService.getLeaderboardByDifficulty(Difficulty.HARD)));
    }
    /**
     * Gestisce il click sul pulsante "Indietro" e ritorna al menu principale.
     *
     */

    @FXML
    private void back() {
        ViewLoader.load(ViewLoader.View.MENU);
    }
}
