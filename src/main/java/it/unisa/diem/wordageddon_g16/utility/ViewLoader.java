package it.unisa.diem.wordageddon_g16.utility;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;

/**
 * Utility per il caricamento dinamico delle viste FXML nell'applicazione.
 * <p>
 * Gestisce l'associazione tra controller e viste, permette di impostare lo {@link Stage} principale,
 * e fornisce metodi statici per cambiare scena visualizzata. Utilizza {@link FXMLLoader} per caricare le viste
 * e una {@link Callback} per creare i controller corretti.
 */
public class ViewLoader {
    private static Callback<Class<?>, Object> controllerFactory;
    private static Stage stage;
    /**
     * Enumerazione delle viste disponibili nell'applicazione.
     * <p>
     * Ogni valore corrisponde al nome del file FXML da caricare.
     * Utilizzato dal metodo {@link ViewLoader#load(ViewLoader.View)}.
     */
    public enum View{
        AUTH("authentication"),
        MENU("menu"),
        GAME("game"),
        USER_PANEL("userPanel"),
        LEADERBOARD("leaderboard");

        private final String viewName;

        /**
         * Costruttore della Enum {@code ViewLoader}.
         *
         * @param viewName nome del file FXML da caricare
         */
        View(String viewName) {
            this.viewName = viewName;
        }

        public String get() {
            return viewName;
        }
    }

    /**
     * Imposta la factory per la creazione dei controller.
     * <p>
     * Utilizzata in {@link it.unisa.diem.wordageddon_g16.WordageddonApp} per istanziare
     * correttamente i controller associati alle viste.
     *
     * @param factory callback responsabile della creazione dei controller
     */
    public static void setControllerFactory(Callback<Class<?>, Object> factory) {
        controllerFactory = factory;
    }

    /**
     * Imposta lo {@link Stage} principale dell'applicazione su cui verranno caricate le viste.
     *
     * @param stage stage da utilizzare per il rendering delle scene
     */
    public static void setStage(Stage stage) {
        ViewLoader.stage = stage;
    }

    /**
     * Carica la vista FXML indicata e la imposta come radice della scena corrente.
     * <p>
     * Utilizza il {@link FXMLLoader} con la factory dei controller e aggiorna la scena dello {@link Stage}
     * con il contenuto corrispondente alla vista richiesta.
     * </p>
     *
     * @param view vista da caricare (uno dei valori di {@link ViewLoader.View})
     * @throws IllegalStateException se {@code stage} o {@code controllerFactory} non sono inizializzati
     * @throws RuntimeException se il file FXML non può essere caricato correttamente
     */
    public static void load(View view){
        if (controllerFactory == null || stage == null) {
            throw new IllegalStateException("ViewLoader not properly initialized.");
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ViewLoader.class.getResource(Resources.RES_PATH+"fxml/" + view.get() + ".fxml"));
            fxmlLoader.setControllerFactory(controllerFactory);
            stage.getScene().setRoot(fxmlLoader.load());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML view: " + view.get(), e);
        }
    }
}