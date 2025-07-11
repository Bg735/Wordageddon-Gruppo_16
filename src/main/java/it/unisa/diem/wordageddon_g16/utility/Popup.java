package it.unisa.diem.wordageddon_g16.utility;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @class PopupBuilder
 * @brief Classe di utilità per creare popup modali JavaFX con layout VBox.
 * <p>
 * Facilita la costruzione e visualizzazione di popup riutilizzabili con stile uniforme.
 */
public class Popup {
    private final Stage stage;
    private final VBox root;

    public Popup(String title, int width, int height) {
        this.stage = new Stage();
        this.root = new VBox(15);
        this.root.setAlignment(Pos.CENTER);
        this.root.setPadding(new Insets(15));
        this.root.setAlignment(Pos.CENTER);
        this.stage.setTitle(title);
        this.stage.setHeight(height);
        this.stage.setWidth(width);
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setResizable(false);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Resources.getStyle("popup"));
        stage.setScene(scene);


    }

    public Popup(String title){
        this(title, 450, 350);
    }

    public Popup addAll(Node... content) {
        root.getChildren().addAll(content);
        return this;
    }

    public Popup addAll(Node content) {
        root.getChildren().add(content);
        return this;
    }

    public void show() {

        stage.showAndWait();
    }

    public Stage getStage() {
        return stage;
    }
}
