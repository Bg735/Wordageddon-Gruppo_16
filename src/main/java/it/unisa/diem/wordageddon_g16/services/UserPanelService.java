package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.*;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPanelService {
    private final GameReportDAO gameReportDAO;
    private final UserDAO userDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopWordDAO;
    private final AppContext appContext;

    public AppContext getAppContext() {
        return appContext;
    }

    public UserPanelService(GameReportDAO gameReportDAO, UserDAO userDAO, DocumentDAO documentDAO, StopWordDAO stopWordDAO, AppContext appContext) {
        this.gameReportDAO = gameReportDAO;
        this.userDAO = userDAO;
        this.documentDAO = documentDAO;
        this.stopWordDAO = stopWordDAO;
        this.appContext = appContext;
    }

    public List<GameReport> getCurrentUserReports() {
        return gameReportDAO.selectAll().stream()
                .filter(r -> r.getUser().getName().equals(appContext.getCurrentUser().getName()))
                .toList();
    }
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

    public void promoteUser(String username) {
        userDAO.selectById(username).ifPresent(user -> {
            user.setAdmin(true);
            userDAO.update(user);
        });
    }

    public void demoteUser(String username) {
        userDAO.selectById(username).ifPresent(user -> {
            user.setAdmin(false);
            userDAO.update(user);
        });
    }


    public List<User> getAllUsersExceptCurrent() {
        String currentUsername = appContext.getCurrentUser().getName();
        return userDAO.selectAll().stream().filter(user -> !user.getName().equals(currentUsername))
                .toList();
    }

    public void addStopwordsFromFile(File file) throws IOException {
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bf.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    stopWordDAO.insert(word);
                }
            }
        } catch (IOException e) {
            SystemLogger.log("Errore della lettura di stopwords", e);
        }
    }
    public List<Document> getAllDocuments() {
        return documentDAO.selectAll();
    }

    public Document addDocument(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String content = String.join(" ", lines);
            int wordCount = content.trim().split("\\s+").length;
            String title = file.getName();
            String path = file.getAbsolutePath();

            Document doc = new Document(0, title, path, wordCount);
            documentDAO.insert(doc);

            return documentDAO.selectAll().stream()
                    .filter(d -> d.getTitle().equals(title) && d.getPath().equals(path))
                    .max(Comparator.comparingLong(Document::getId))
                    .orElseThrow(() -> new RuntimeException("Documento non trovato dopo inserimento."));

        } catch (IOException e) {
            SystemLogger.log("Errore lettura file", e);
            throw new RuntimeException("Errore nella lettura del file");
        }
    }




    public void deleteDocument(Document doc) {
        documentDAO.delete(doc);
    }

    public List<String> getAllStopwords() {
        return stopWordDAO.selectAll();
    }

    public void addStopWords(String word) {
        stopWordDAO.insert(word);
    }

    public void deleteStopword(String word) {
        stopWordDAO.delete(word);
    }
}