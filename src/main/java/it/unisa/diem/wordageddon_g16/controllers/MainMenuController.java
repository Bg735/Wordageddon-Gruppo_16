package it.unisa.diem.wordageddon_g16.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class MainMenuController {
    public <T> MainMenuController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unisa/diem/wordaggedon_g16/fmxl/menu.fxml"));
        Parent root = loader.load();
    }
}
