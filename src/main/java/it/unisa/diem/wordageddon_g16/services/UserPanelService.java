package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCStopWordDAO;
import it.unisa.diem.wordageddon_g16.db.JDBCUserDAO;
import it.unisa.diem.wordageddon_g16.db.JDBCWdmDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.GameReportDAO;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * /**
 * Service per la gestione delle funzionalità accessibili dal pannello utente.
 * Fornisce metodi per informazioni sui report di gioco, gestione documenti, stopwords e admin.
 */
public class UserPanelService {
    private final GameReportDAO gameReportDAO;
    private final JDBCUserDAO userDAO;
    private final DocumentDAO documentDAO;
    private final JDBCStopWordDAO stopWordDAO;
    private final AppContext appContext;
    private final JDBCWdmDAO wdmDAO;

    /**
     * Costruttore del servizio.
     *
     * @param gameReportDAO DAO per i report di gioco
     * @param userDAO       DAO per gli utenti
     * @param documentDAO   DAO per i documenti
     * @param stopWordDAO   DAO per le stopword
     * @param appContext    Contesto applicativo
     */
    public UserPanelService(GameReportDAO gameReportDAO, JDBCUserDAO userDAO, DocumentDAO documentDAO, JDBCStopWordDAO stopWordDAO, JDBCWdmDAO wdmDAO, AppContext appContext) {
        this.gameReportDAO = gameReportDAO;
        this.userDAO = userDAO;
        this.documentDAO = documentDAO;
        this.stopWordDAO = stopWordDAO;
        this.appContext = appContext;
        this.wdmDAO = wdmDAO;
    }

    public void loadWDM(WDM wdm) {
        // Controllo se il documento è già presente
        if (wdmDAO.selectById(wdm.getDocument()).isPresent()) {
            documentDAO.update(wdm.getDocument());
            wdmDAO.update(wdm);
        }
        else {
            // Inserisco il documento nel database
            documentDAO.insert(wdm.getDocument());
            // Inserisco la matrice WDM
            wdmDAO.insert(wdm);
        }
    }

    /**
     * Recupera tutti i report dell'utente attualmente loggato.
     *
     * @return lista di GameReport
     */
    public List<GameReport> getCurrentUserReports() {
        return gameReportDAO.selectAll().stream()
                .filter(r -> r.user().getName().equals(appContext.getCurrentUser().getName()))
                .toList();
    }

    /**
     * Calcola statistiche sull'utente corrente: punteggio massimo, medio e numero di partite.
     *
     * @return mappa con chiavi "maxScore", "averageScore", "totalGames"
     */
    public Map<String, Object> getUserStatsForCurrentUser() {
        List<GameReport> reports = getCurrentUserReports();

        int max = reports.stream().mapToInt(GameReport::score)
                .max().orElse(0);

        double average = reports.stream().mapToInt(GameReport::score)
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
        return userDAO.selectAll().stream().filter(user -> !user.getName().equals(currentUsername)).toList();
    }

    /**
     * Aggiunge stopword leggendo da un file.
     *
     * @param file file .txt contenente le stopwords
     */
    public void addStopwordsFromFile(File file) throws IOException {
        // prelevo le stopwords dal database
        Set<String> stopWordsSet = new HashSet<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bf.readLine()) != null) {
                stopWordsSet.addAll(stopWordsParser(line));
            }
        }
        for (String stopWord : stopWordsSet) {
            stopWordDAO.insert(stopWord);
        }
    }

    /**
     * Converte un nome file in un titolo simbolico con la prima lettera maiuscola di ogni parola.
     * Esempio: "mario_rossi.txt" -> "Mario Rossi"
     *
     * @param filename il nome del file (può contenere estensione e underscore)
     * @return il titolo simbolico
     */
    public String symbolicNameOf(String filename) {
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

    /**
     * Restituisce tutti i documenti presenti nel sistema.
     *
     * @return lista di documenti
     */
    public Collection<Document> getAllDocuments() {
        return documentDAO.selectAll();
    }

    /**
     * Aggiunge un nuovo documento al db, se non è già presente.
     *
     * @param tempFile il file da caricare
     * @return il documento aggiunto o null se già esiste
     */
    public void addDocument(File tempFile) throws IOException {
        Path docsDir = Resources.getDocsDirPath();
        String filename = tempFile.getName();
        Path filePath = docsDir.resolve(filename);

        // Controllo se il documento é giá presente nel database
        if (documentDAO.selectById(filename).isPresent()) {
            SystemLogger.log("Documento già presente: " + filename, null);
            throw new FileAlreadyExistsException("Documento già presente: " + filename);
        }

        // Creo la cartella e copia il file
        Files.createDirectories(docsDir);
        Files.copy(tempFile.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Elimina un documento dal sistema.
     *
     * @param doc il documento da eliminare
     */
    public void deleteDocument(Document doc) {
        documentDAO.delete(doc);

        // Se il documento non è più presente nel database, elimino il file fisico
        if (documentDAO.selectById(doc).isEmpty()) {
            try {
                Files.deleteIfExists(Resources.getDocPath(doc));
            } catch (IOException e) {
                SystemLogger.log("Error deleting" + doc, e);
                throw new RuntimeException("Error deleting" + doc, e);
            }
        }

    }

    /**
     * Recupera tutte le stopword.
     *
     * @return lista di stringhe con le stopwords
     */
    public Set<String> getStopwords() {
        return stopWordDAO.selectAll();
    }

    /**
     * Aggiunge le stopwords al sistema. Il valore di ritorno consente l'aggiunta delle stopwords alla lista visualizzata a schermo
     *
     * @param tfRaw rappresenta il valore grezzo del campo di testo in cui sono inserite le stopwords
     */
    public void addStopWords(String tfRaw) {
        Set<String> stopWordsSet = stopWordsParser(tfRaw);

        // Inserisce le nuove stopword nel database (il controllo duplicati è gestito dal DB)
        for (String stopWord : stopWordsSet) {
            stopWordDAO.insert(stopWord);
        }
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
        return stopWordsSet;
    }


}