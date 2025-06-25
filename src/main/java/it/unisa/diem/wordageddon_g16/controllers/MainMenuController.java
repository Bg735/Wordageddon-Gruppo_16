package it.unisa.diem.wordageddon_g16.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;


public class MainMenuController {
    @FXML
    private Button leaderBoardBtn;

    @FXML
    private Button startBtn;

    @FXML
    private Label usernameLabel;

    @FXML
    void handleStartBtn(ActionEvent event) {

    }

    @FXML
    void handleleaderBoardBtn(ActionEvent event) {

    }

    public <T> MainMenuController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unisa/diem/wordaggedon_g16/fmxl/menu.fxml"));
        Parent root = loader.load();
    }
}
