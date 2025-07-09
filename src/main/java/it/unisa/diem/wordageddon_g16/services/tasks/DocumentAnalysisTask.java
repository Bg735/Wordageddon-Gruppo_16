package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.concurrent.Task;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Task JavaFX che si occupa dell'analisi asincrona di un documento testuale.
 * <p>
 * Questo task esegue le seguenti operazioni:
 * <ul>
 *   <li>Verifica se il documento è già presente nel database (tramite titolo e path).</li>
 *   <li>Se il documento non esiste, legge il file dal file system e conta il numero di parole (escludendo la punteggiatura).</li>
 *   <li>Crea e inserisce il documento nel database. Poiché l'id viene generato automaticamente dal database (autoincremento), il documento viene recuperato nuovamente per ottenere l'id assegnato.</li>
 *   <li>Costruisce la matrice delle parole significative (WDM) per il documento e la inserisce nel database.</li>
 *   <li>Restituisce l'oggetto {@link WDM} risultante oppure {@code null} se il documento esiste già o si verifica un errore di lettura.</li>
 * </ul>
 * Tutte le operazioni vengono eseguite in background per non bloccare la GUI JavaFX.
 */
public class DocumentAnalysisTask extends Task<WDM> {
    private final Path filePath;
    private final String title;
    private final DocumentDAO documentDAO;
    private final WdmDAO wdmDAO;
    private final Collection<String> stopWords;

    /**
     * Costruttore del task di analisi documento.
     *
     * @param filePath    percorso del file da analizzare
     * @param documentDAO DAO per la gestione dei documenti
     * @param wdmDAO      DAO per la gestione delle matrici WDM
     * @param stopWords   insieme delle stopwords da escludere dall'analisi
     */
    public DocumentAnalysisTask(Path filePath, DocumentDAO documentDAO, WdmDAO wdmDAO, Collection<String> stopWords) {
        this.filePath = filePath;
        this.documentDAO = documentDAO;
        this.wdmDAO = wdmDAO;
        this.stopWords = stopWords;
        title = filePath.getFileName().toString();
    }

    /**
     * Esegue l'analisi del documento:
     * <ul>
     *   <li>Verifica la presenza del documento.</li>
     *   <li>Legge il file e conta le parole (rimuovendo la punteggiatura).</li>
     *   <li>Inserisce il documento e la relativa matrice WDM nel database.</li>
     * </ul>
     * Se il documento esiste già o si verifica un errore di lettura, restituisce {@code null}.
     *
     * @return la matrice delle parole {@link WDM} generata, oppure {@code null} se il documento esiste già o in caso di errore
     */
    @Override
    protected WDM call() {
        // Verifica se il documento esiste già
        boolean alreadyExists = documentDAO.selectAll().stream()
                .anyMatch(d -> d.title().equals(title) &&
                        Paths.get(d.path()).normalize().toAbsolutePath()
                                .equals(filePath.normalize().toAbsolutePath()));
        if (alreadyExists) {
            System.out.println("Document already exists in the database: " + title);
            return null;
        }


        // Effettuo il parsing del file e conto le parole
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            SystemLogger.log("Error during reading file for analysis in thread [" + Thread.currentThread().getName() + "]", e);
            return null;
        }

        String content = String.join(" ", lines);
        String cleanContent = content.replaceAll("\\p{Punct}", ""); // rimuovo la punteggiatura
        int wordCount = cleanContent.trim().split("\\s+").length;

        // Creo e inserisco il documento (l'id é inizializzato ad 1 dal costruttore
        Document doc = new Document(title, filePath.toString(), wordCount);
        documentDAO.insert(doc);

        // Usando chiavi con autoincremento su sqlite, devo prelevare nuovamente il documento inserito per ottenere l'id corretto
        Document insertedDoc = documentDAO.selectAll().stream()
                .filter(d -> d.title().equals(title) && d.path().equals(filePath.toString()))
                .max(Comparator.comparingLong(Document::id))
                .orElseThrow(() -> new RuntimeException("Document not found after insertion"));

        // Calcola la matrice delle parole significative
        WDM wdm = new WDM(insertedDoc, stopWords);
        wdmDAO.insert(wdm);
        return wdm;
    }

}
