package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.*;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;

import java.util.List;

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
    public List<UserPanelEntry> getUserPanelEntriesForCurrentUser() {
        return getCurrentUserReports().stream()
                .map(report -> new UserPanelEntry(
                        report.getDifficulty().name(),
                        report.getScore(),
                        String.format("%02d:%02d", report.getUsedTime().toMinutesPart(), report.getUsedTime().toSecondsPart())
                ))
                .toList();
    }

    public List<User> getAllUsers() {
        return userDAO.selectAll();
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


    public List<Document> getAllDocuments() {
        return documentDAO.selectAll();
    }

    public void addDocument(Document doc) {
        documentDAO.insert(doc);
    }

    public void deleteDocument(Document doc) {
        documentDAO.delete(doc);
    }

    public List<String> getAllStopwords() {
        return stopWordDAO.selectAll();
    }

    public void addStopword(String word) {
        stopWordDAO.insert(word);
    }

    public void deleteStopword(String word) {
        stopWordDAO.delete(word);
    }
}
