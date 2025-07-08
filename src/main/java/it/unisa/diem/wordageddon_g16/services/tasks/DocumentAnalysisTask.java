package it.unisa.diem.wordageddon_g16.services.tasks;

import it.unisa.diem.wordageddon_g16.db.DocumentDAO;
import it.unisa.diem.wordageddon_g16.models.Document;
import javafx.concurrent.Task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

// Task JavaFX per l'analisi del documento
public class DocumentAnalysisTask extends Task<Document> {
    private final Path filePath;
    private final DocumentDAO documentDAO;

    public DocumentAnalysisTask(Path filePath, DocumentDAO documentDAO) {
        this.filePath = filePath;
        this.documentDAO = documentDAO;
    }

    @Override
    protected Document call() throws Exception {
        List<String> lines = Files.readAllLines(filePath);
        String content = String.join(" ", lines);
        int wordCount = content.trim().split("\\s+").length;

        String title = filePath.getFileName().toString();
}
