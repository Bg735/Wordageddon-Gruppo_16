package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Collection;

/**
 * Service JavaFX che analizza un documento testuale in background,
 * producendo una Word Document Matrix (WDM) che associa il documento
 * alle frequenze delle parole significative (escludendo le stopwords).
 *
 * Questo service esegue l'analisi in modo asincrono, evitando di bloccare
 * l'interfaccia grafica durante l'elaborazione.
 */
public class DocAnalyzerServiceFX extends Service<WDM> {
    /**
     * Il documento da analizzare.
     */
    private final Document document;

    /**
     * Collezione delle stopwords da escludere dall'analisi.
     */
    private final Collection<String> stopWords;

    /**
     * DAO per l'accesso alle stopwords dal database.
     */
    private final StopWordDAO stopWordDAO;

    /**
     * Costruttore del service.
     *
     * @param document  il documento da analizzare
     */
    public DocAnalyzerServiceFX(Document document, StopWordDAO stopWordDAO) {
        this.document = document;
        this.stopWordDAO = stopWordDAO;
        stopWords = stopWordDAO.selectAll();
    }

    /**
     * Crea il task che effettua l'analisi del documento in background.
     * Il task restituisce un oggetto WDM contenente le frequenze delle parole.
     *
     * @return un Task che produce un oggetto WDM al termine dell'analisi
     */
    @Override
    protected Task<WDM> createTask() {
        return new Task<WDM>() {
            /**
             * Esegue l'analisi del documento e restituisce un oggetto WDM.
             *
             * @return il risultato dell'analisi (Word Document Matrix)
             * @throws Exception se si verifica un errore durante l'analisi
             */
            @Override
            protected WDM call() throws Exception {
                // Analizza il documento e restituisce un WDM
                return new WDM(document, stopWords);
            }

            /**
             * Gestisce eventuali errori avvenuti durante l'esecuzione del task.
             * Questo metodo viene chiamato automaticamente se il task fallisce.
             */
            @Override
            protected void failed() {
                super.failed();
                Throwable exception = getException();
                if (exception != null) {
                    System.err.println("Error during document analysis: " + exception.getMessage());
                    SystemLogger.log("Error during document analysis:", exception);
                }
            }
        };
    }
}
