package it.unisa.diem.wordageddon_g16.controllers;

import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.services.LeaderboardService;
import it.unisa.diem.wordageddon_g16.services.ViewLoader;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class LeaderboardController implements Initializable {

    @FXML
    private ImageView backButton;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> globalTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> easyTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> mediumTW;

    @FXML
    private TableView<LeaderboardService.LeaderboardEntry> hardTW;

    private final LeaderboardService leaderboardService;

    public LeaderboardController(AppContext context) {
        this.leaderboardService = context.getLeaderboardService();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        for (TableView<LeaderboardService.LeaderboardEntry> tableView : new TableView[]{globalTW, easyTW, mediumTW, hardTW}) {
            tableView.setPlaceholder(new javafx.scene.control.Label("Nessun dato disponibile"));

            var indexCol = (TableColumn<LeaderboardService.LeaderboardEntry, Integer>) tableView.getColumns().getFirst();
            indexCol.setCellFactory(col -> new TableCell<>() {
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

    @FXML
    private void back(ActionEvent event) {
        ViewLoader.load(ViewLoader.View.MENU);
    }
}
