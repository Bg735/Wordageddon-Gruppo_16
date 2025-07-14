package it.unisa.diem.wordageddon_g16.utility;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Classe di utilità per creare popup modali JavaFX con layout {@link VBox}.
 * <p>
 * Facilita la costruzione e visualizzazione di popup riutilizzabili con stile uniforme.
 * I popup sono centrati, non ridimensionabili e con padding predefinito.
 * È possibile aggiungere contenuti dinamici e applicare lo stile {@code popup.css}.
 */
public class Popup {
    private final Stage stage;
    private final VBox root;

    /**
     * Costruisce un popup modale con titolo, larghezza e altezza personalizzati.
     * <p>
     * Il contenuto è gestito tramite un layout {@link VBox}, centrato e stilizzato,
     * e il popup viene inizializzato con {@link Modality#APPLICATION_MODAL}.
     * </p>
     *
     * @param title  titolo della finestra
     * @param width  larghezza in pixel
     * @param height altezza in pixel
     */
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

    /**
     * Costruisce un popup modale con dimensioni predefinite.
     * <p>
     * Larghezza: 450 px<br>
     * Altezza: 350 px
     * </p>
     *
     * @param title titolo della finestra
     */
    public Popup(String title){
        this(title, 450, 350);
    }

    /**
     * Aggiunge uno o più nodi grafici al contenuto del popup.
     *
     * @param content array di nodi da inserire nel layout
     * @return istanza di {@code Popup} per chiamate fluide
     */
    public Popup addAll(Node... content) {
        root.getChildren().addAll(content);
        return this;
    }

    /**
     * Aggiunge un singolo nodo grafico al contenuto del popup.
     *
     * @param content nodo da inserire nel layout
     * @return istanza di {@code Popup} per chiamate fluide
     */
    public Popup addAll(Node content) {
        root.getChildren().add(content);
        return this;
    }

    /**
     * Mostra il popup modale e attende la chiusura da parte dell'utente.
     * <p>
     * Utilizza {@code stage.showAndWait()} per bloccare il flusso finché il popup non viene chiuso.
     * </p>
     */
    public void show() {

        stage.showAndWait();
    }

    /**
     * Restituisce lo {@link Stage} interno del popup, utile per personalizzazioni avanzate.
     *
     * @return riferimento allo stage del popup
     */
    public Stage getStage() {
        return stage;
    }
}
