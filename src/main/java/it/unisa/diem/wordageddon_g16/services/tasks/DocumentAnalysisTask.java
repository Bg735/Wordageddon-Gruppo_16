package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import javafx.concurrent.Task;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class DocumentAnalysisTask extends Task<WDM> {
    private final Path filePath;
    private final String title;
    private final DocumentDAO documentDAO;
    private final Collection<String> stopWords;

    public DocumentAnalysisTask(Path filePath, String title, DocumentDAO documentDAO, Collection<String> stopWords) {
        this.filePath = filePath;
        this.title = title;
        this.documentDAO = documentDAO;
        this.stopWords = stopWords;
    }

    @Override
    protected WDM call() throws Exception {
        // Leggi il file e calcola il numero di parole
        List<String> lines = Files.readAllLines(filePath);
        String content = String.join(" ", lines);
        int wordCount = content.trim().split("\\s+").length;

        // Verifica se il documento esiste già
        boolean alreadyExists = documentDAO.selectAll().stream()
                .anyMatch(d -> d.title().equals(title) && d.filename().equals(filePath.toString()));
        if (alreadyExists) {
            return null;
        }

        // Crea e inserisci il documento
        Document doc = new Document(0, title, filePath.toString(), wordCount);
        documentDAO.insert(doc);
        Document insertedDoc = documentDAO.selectAll().stream()
                .filter(d -> d.title().equals(title) && d.filename().equals(filePath.toString()))
                .max(Comparator.comparingLong(Document::id))
                .orElseThrow(() -> new RuntimeException("Documento non trovato dopo inserimento."));

        // Analizza e crea la WDM
        return new WDM(insertedDoc, stopWords);
    }
}
