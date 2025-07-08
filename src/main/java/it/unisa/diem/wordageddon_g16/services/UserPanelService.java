package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.*;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.tasks.DocumentAnalysisTask;

import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @class UserPanelService
 *
 * Service per la gestione delle funzionalità accessibili dal pannello utente.
 * Fornisce metodi per informazioni sui report di gioco, gestione documenti, stopwords e admin.
 */
public class UserPanelService {
    private final GameReportDAO gameReportDAO;
    private final UserDAO userDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopWordDAO;
    private final AppContext appContext;

    /**
     * Costruttore del servizio.
     *
     * @param gameReportDAO DAO per i report di gioco
     * @param userDAO DAO per gli utenti
     * @param documentDAO DAO per i documenti
     * @param stopWordDAO DAO per le stopword
     * @param appContext Contesto applicativo
     */
    public UserPanelService(GameReportDAO gameReportDAO, UserDAO userDAO, DocumentDAO documentDAO, StopWordDAO stopWordDAO, AppContext appContext) {
        this.gameReportDAO = gameReportDAO;
        this.userDAO = userDAO;
        this.documentDAO = documentDAO;
        this.stopWordDAO = stopWordDAO;
        this.appContext = appContext;
    }

    /**
     * Recupera tutti i report dell'utente attualmente loggato.
     *
     * @return lista di GameReport
     */
    public List<GameReport> getCurrentUserReports() {
        return gameReportDAO.selectAll().stream()
                .filter(r -> r.getUser().getName().equals(appContext.getCurrentUser().getName()))
                .toList();
    }

    /**
     * Calcola statistiche sull'utente corrente: punteggio massimo, medio e numero di partite.
     *
     * @return mappa con chiavi "maxScore", "averageScore", "totalGames"
     */
    public Map<String, Object> getUserStatsForCurrentUser() {
        List<GameReport> reports = getCurrentUserReports();

        int max = reports.stream().mapToInt(GameReport::getScore)
                .max().orElse(0);

        double average = reports.stream().mapToInt(GameReport::getScore)
                .average().orElse(0.0);

        int total = reports.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("maxScore", max);
        stats.put("averageScore", average);
        stats.put("totalGames", total);
        return stats;
    }

    /**
     * Promuove un utente a ruolo admin.
     *
     * @param username nome dell'utente da promuovere
     */
    public void promoteUser(String username) {
        userDAO.selectById(username).ifPresent(user -> {
            user.setAdmin(true);
            userDAO.update(user);
        });
    }

    /**
     * Retrocede un utente da admin a utente normale.
     *
     * @param username nome dell'utente da retrocedere
     */
    public void demoteUser(String username) {
        userDAO.selectById(username).ifPresent(user -> {
            user.setAdmin(false);
            userDAO.update(user);
        });
    }

    /**
     * Recupera tutti gli utenti escluso quello attualmente loggato.
     *
     * @return lista di utenti
     */
    public List<User> getAllUsersExceptCurrent() {
        String currentUsername = appContext.getCurrentUser().getName();
        return userDAO.selectAll().stream().filter(user -> !user.getName().equals(currentUsername))
                .toList();
    }

    /**
     * Aggiunge stopword leggendo da un file.
     *
     * @param file file .txt contenente le stopwords
     */
    public void addStopwordsFromFile(File file) throws IOException {
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bf.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    // le stopwords sono salvate soltanto a livello db
                    stopWordDAO.insert(word);
                }
            }
        } catch (IOException e) {
            SystemLogger.log("Errore della lettura di stopwords", e);
        }
    }

    /**
     * Restituisce tutti i documenti presenti nel sistema.
     *
     * @return lista di documenti
     */
    public List<Document> getAllDocuments() {
        return documentDAO.selectAll();
    }

    /**
     * Aggiunge un nuovo documento al db, se non è già presente.
     *
     * @param tempFile il file da caricare
     * @return il documento aggiunto o null se già esiste
     */
    public Document addDocument(File tempFile) {
        try {
            // Copio il file in una cartella dedicata disponibile esternamente al jar
            Path docsDir = Paths.get("uploads/documents");
            String title = tempFile.getName();

            Path targetPath = docsDir.resolve(title);
            Files.copy(tempFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            File copiedFile = targetPath.toFile();  // file copiato nella cartella uploads/documents

            boolean alreadyExists = documentDAO.selectAll().stream()
                    .anyMatch(d -> d.getTitle().equals(title) && d.getPath().equals(targetPath.toString()));

            if (alreadyExists) {
                return null;
            }

            Document doc = new Document(0, title, targetPath.toString(), wordCount);
            documentDAO.insert(doc);

            DocumentAnalysisTask analysisTask = new DocumentAnalysisTask(targetPath);
            analysisTask.setOnSucceeded(event -> {
                Document analyzedDocument = analysisTask.getValue();
            });

            analysisTask.setOnFailed(event -> {
                SystemLogger.log("Errore nell'analisi del documento", analysisTask.getException());
            });

            return documentDAO.selectAll().stream()
                    .filter(d -> d.getTitle().equals(title) && d.getPath().equals(targetPath.toString()))
                    .max(Comparator.comparingLong(Document::getId))
                    .orElseThrow(() -> new RuntimeException("Documento non trovato dopo inserimento."));

        } catch (IOException e) {
            SystemLogger.log("Errore lettura file", e);
            throw new RuntimeException("Errore nella lettura del file");
        }
    }

    /**
     * Elimina un documento dal sistema.
     *
     * @param doc il documento da eliminare
     */
    public void deleteDocument(Document doc) {
        documentDAO.delete(doc);
    }

    /**
     * Recupera tutte le stopword.
     *
     * @return lista di stringhe con le stopwords
     */
    public List<String> getAllStopwords() {
        return stopWordDAO.selectAll();
    }

    /**
     * Aggiunge una singola stopword.
     *
     * @param word la parola da aggiungere
     */
    public void addStopWords(String word) {
        stopWordDAO.insert(word);
    }

    /**
     * Rimuove una stopword dal sistema.
     *
     * @param word la parola da rimuovere
     */
    public void deleteStopword(String word) {
        stopWordDAO.delete(word);
    }
}