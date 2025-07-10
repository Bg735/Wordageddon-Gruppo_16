package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.WdmDAO;
import it.unisa.diem.wordageddon_g16.services.Resources;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.concurrent.Task;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

/**
 * Task JavaFX che si occupa dell'analisi asincrona di un documento testuale.
 * <p>
 * Questo task esegue le seguenti operazioni:
 * <ul>
 *   <li>Verifica se il documento è già presente nel database (tramite titolo e filename).</li>
 *   <li>Se il documento non esiste, legge il file dal file system e conta il numero di parole (escludendo la punteggiatura).</li>
 *   <li>Crea e inserisce il documento nel database. Poiché l'id viene generato automaticamente dal database (autoincremento), il documento viene recuperato nuovamente per ottenere l'id assegnato.</li>
 *   <li>Costruisce la matrice delle parole significative (WDM) per il documento e la inserisce nel database.</li>
 *   <li>Restituisce l'oggetto {@link WDM} risultante oppure {@code null} se il documento esiste già o si verifica un errore di lettura.</li>
 * </ul>
 * Tutte le operazioni vengono eseguite in background per non bloccare la GUI JavaFX.
 */
public class DocumentAnalysisTask extends Task<WDM> {
    private final DocumentDAO documentDAO;
    private final WdmDAO wdmDAO;
    private final StopWordDAO stopWordDAO;
    private final File tempFile;

    public DocumentAnalysisTask(File tempFile, DocumentDAO documentDAO, WdmDAO wdmDAO, StopWordDAO stopWordDAO) {
        this.documentDAO = documentDAO;
        this.wdmDAO = wdmDAO;
        this.stopWordDAO = stopWordDAO;
        this.tempFile = tempFile;
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
        // prelevo le stopwords dal database
        Set<String> stopWords = stopWordDAO.selectAll();

        Path docsDir = Resources.getDocsPath();
        String filename = tempFile.getName();
        Path filePath = docsDir.resolve(filename);

        // Controllo se il documento é giá presente nel database
        boolean alreadyExists = documentDAO.selectById(filename).isPresent();
        if (alreadyExists) {
            SystemLogger.log("Documento già presente: " + filename, null);
            throw new RuntimeException("Documento già presente: " + filename);
        }

        // Creo un nome simbolico per il documento
        String title = toSymbolicTitle(filename);

        // Creo la cartella e copia il file
        try {
            Files.createDirectories(docsDir);
            Files.copy(tempFile.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SystemLogger.log("Errore copia file", e);
            throw new RuntimeException("Errore durante la copia del file: " + e.getMessage(), e);
        }

        // Leggo il file e conto le parole
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            SystemLogger.log("Errore lettura file", e);
            throw new RuntimeException("Errore durante la lettura file: " + e.getMessage(), e);
        }

        String content = String.join(" ", lines);
        String cleanContent = content.replaceAll("\\p{Punct}", "");
        int wordCount = cleanContent.trim().split("\\s+").length;

        // Inserisco nel db il documento e la matrice WDM
        Document doc = new Document(filename,title, wordCount);
        documentDAO.insert(doc);

        WDM wdm = new WDM(doc, stopWords);
        wdmDAO.insert(wdm);
        return wdm;
    }

    /**
     * Converte un nome file in un titolo simbolico con la prima lettera maiuscola di ogni parola.
     * Esempio: "mario_rossi.txt" -> "Mario Rossi"
     *
     * @param filename il nome del file (può contenere estensione e underscore)
     * @return il titolo simbolico
     */
    private String toSymbolicTitle(String filename) {
        // Rimuovo l'estensione, se presente
        int dotIndex = filename.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? filename : filename.substring(0, dotIndex);

        // Sostituisco _ o - con spazio
        String[] words = baseName.replaceAll("[_-]", " ").toLowerCase().split(" ");
        StringBuffer sb = new StringBuffer();

        // Capitalizza la prima lettera di ogni parola
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

}
