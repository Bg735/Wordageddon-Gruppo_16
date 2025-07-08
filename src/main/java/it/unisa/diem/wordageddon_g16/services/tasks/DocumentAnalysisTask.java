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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class DocumentAnalysisTask extends Task<WDM> {
    private final Path filePath;
    private final String title;
    private final DocumentDAO documentDAO;
    private final WdmDAO wdmDAO;
    private final Collection<String> stopWords;

    public DocumentAnalysisTask(Path filePath, DocumentDAO documentDAO, WdmDAO wdmDAO, Collection<String> stopWords) {
        this.filePath = filePath;
        this.documentDAO = documentDAO;
        this.wdmDAO = wdmDAO;
        this.stopWords = stopWords;
        title = filePath.getFileName().toString();
    }

    @Override
    protected WDM call() {
        // Verifica se il documento esiste già
        boolean alreadyExists = documentDAO.selectAll().stream()
                .anyMatch(d -> d.title().equals(title) && d.path().equals(filePath.toString()));
        if (alreadyExists) {
            // Se esiste, non analizzo nuovamente
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

        // Creo e inserisci il documento (l'id é inizializzato ad 1 dal costruttore
        Document doc = new Document(title, filePath.toString(), wordCount);
        documentDAO.insert(doc);

        // Calcola la matrice delle parole significative
        WDM wdm = new WDM(doc, stopWords);
        wdmDAO.insert(wdm);

        return wdm;
    }


}
