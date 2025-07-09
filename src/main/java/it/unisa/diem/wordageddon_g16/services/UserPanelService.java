package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.*;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.services.tasks.DocumentAnalysisTask;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * /**
 * Service per la gestione delle funzionalità accessibili dal pannello utente.
 * Fornisce metodi per informazioni sui report di gioco, gestione documenti, stopwords e admin.
 */
public class UserPanelService {
    private final GameReportDAO gameReportDAO;
    private final UserDAO userDAO;
    private final DocumentDAO documentDAO;
    private final StopWordDAO stopWordDAO;
    private final AppContext appContext;
    private final WdmDAO wdmDao;

    /**
     * Costruttore del servizio.
     *
     * @param gameReportDAO DAO per i report di gioco
     * @param userDAO       DAO per gli utenti
     * @param documentDAO   DAO per i documenti
     * @param stopWordDAO   DAO per le stopword
     * @param appContext    Contesto applicativo
     */
    public UserPanelService(GameReportDAO gameReportDAO, UserDAO userDAO, DocumentDAO documentDAO, StopWordDAO stopWordDAO, WdmDAO wdmDAO, AppContext appContext) {
        this.gameReportDAO = gameReportDAO;
        this.userDAO = userDAO;
        this.documentDAO = documentDAO;
        this.stopWordDAO = stopWordDAO;
        this.appContext = appContext;
        this.wdmDao = wdmDAO;
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
    public Task<Set<String>> addStopwordsFromFile(File file) {
        return new Task<>() {
            @Override
            protected Set<String> call() {
                Set<String> stopWordsSet = new HashSet<>();
                try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = bf.readLine()) != null) {
                        stopWordsSet.addAll(stopWordsParser(line));
                    }
                } catch (IOException e) {
                    SystemLogger.log("Error during stopwords file reading", e);
                    throw new RuntimeException("Error during stopwords file reading", e);
                }
                for (String stopWord : stopWordsSet) {
                    stopWordDAO.insert(stopWord);
                }
                return stopWordsSet;
            }
        };
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
    public Task<WDM> addDocument(File tempFile) {
        Task<WDM> task = new DocumentAnalysisTask(tempFile, documentDAO, wdmDao, stopWordDAO);
        new Thread(task).start();
        return task;
    }

    /**
     * Elimina un documento dal sistema.
     *
     * @param doc il documento da eliminare
     */
    public void deleteDocument(Document doc) {
        documentDAO.delete(doc);

        // Se il documento non è più presente nel database, elimino il file fisico
        if (documentDAO.selectById(doc.path()).isEmpty()) {
            try {
                Files.deleteIfExists(doc.path());
            } catch (IOException e) {
                SystemLogger.log("Error during file deletion " + doc.path(), e);
                throw new RuntimeException("Error during file deletion " + doc.path(), e);
            }
        }

    }

    /**
     * Recupera tutte le stopword.
     *
     * @return lista di stringhe con le stopwords
     */
    public Set<String> getAllStopwords() {
        return stopWordDAO.selectAll();
    }

    /**
     * Aggiunge le stopwords al sistema. Il valore di ritorno consente l'aggiunta delle stopwords alla lista visualizzata a schermo
     *
     * @param tfRaw rappresenta il valore grezzo del campo di testo in cui sono inserite le stopwords
     */
    public Set<String> addStopWords(String tfRaw) {
        Set<String> stopWordsSet = stopWordsParser(tfRaw);

        // Inserisce le nuove stopword nel database (il controllo duplicati è gestito dal DB)
        for (String stopWord : stopWordsSet) {
            stopWordDAO.insert(stopWord);
        }

        // Ritorna le stopword appena aggiunte
        return stopWordsSet;
    }

    /**
     * Rimuove una stopword dal sistema.
     *
     * @param word la parola da rimuovere
     */
    public void deleteStopword(String word) {
        stopWordDAO.delete(word);
    }

    /**
     * Estrae tutte le stopword (parole e simboli di punteggiatura) da una singola riga di testo.
     * <p>
     * Il metodo effettua il parsing della stringa fornita, estraendo:
     * <ul>
     *   <li>Le parole (sequenze di caratteri alfabetici) separate da spazi o punteggiatura</li>
     *   <li>Tutti i simboli di punteggiatura presenti</li>
     * </ul>
     * Tutti i token vengono inseriti in un {@code Set<String>} per garantire l’unicità.
     * La stringa viene convertita in minuscolo e ripulita da spazi iniziali/finali.
     *
     * @param tfRaw la riga di testo da analizzare (può contenere parole e simboli)
     * @return un insieme di stopword estratte dalla riga (parole e simboli di punteggiatura)
     */
    private Set<String> stopWordsParser(String tfRaw) {
        Set<String> stopWordsSet = new HashSet<>();
        String input = tfRaw.trim().toLowerCase();

        // Estrae le parole
        String[] stopWords = input.split("[\\p{Punct}\\s]+");
        for (String word : stopWords) {
            if (!word.isEmpty()) {
                stopWordsSet.add(word);
            }
        }

        // Estrae i simboli di punteggiatura
        for (char c : input.toCharArray()) {
            if (String.valueOf(c).matches("\\p{Punct}")) {
                stopWordsSet.add(String.valueOf(c));
            }
        }

        return stopWordsSet;
    }


}