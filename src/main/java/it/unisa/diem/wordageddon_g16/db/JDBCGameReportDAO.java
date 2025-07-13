package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DAO;
import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.UserDAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.db.exceptions.UpdateFailedException;
import it.unisa.diem.wordageddon_g16.models.*;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import javafx.util.Callback;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) per la gestione dei report di gioco (GameReport) nel database.
 * Consente di eseguire operazioni CRUD sulla tabella GameReport e di gestire le relazioni
 * con utenti e documenti associati a ciascun report.
 */
public class JDBCGameReportDAO extends JdbcDAO<GameReport> implements GameReportDAO {

    /**
     * DAO utilizzato per la gestione degli utenti associati ai report di gioco.
     */
    private final UserDAO userDAO;

    /**
     * DAO utilizzato per la gestione dei documenti associati ai report di gioco.
     */
    private final DocumentDAO documentDAO;

    /**
     * Costruisce un nuovo GameReportDAO utilizzando la connessione e i DAO specificati.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     * @param documentDAO il DAO per la gestione dei documenti
     * @param userDAO il DAO per la gestione degli utenti
     */
    public JDBCGameReportDAO(Connection conn, DAO<Document> documentDAO, DAO<User> userDAO) {
        super(conn);
        this.userDAO = (UserDAO) userDAO;
        this.documentDAO = (DocumentDAO) documentDAO;
    }

    /**
     * Recupera un report di gioco dal database tramite il suo identificativo.
     *
* @return un Optional contenente il report trovato, o vuoto se non esiste
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public Optional<GameReport> selectBy(User user, Timestamp timestamp) {
        return selectWhere("user = ? AND timestamp = ?", user, timestamp).stream().findFirst();
    }

    /**
     * Recupera tutti i report di gioco presenti nella tabella GameReport.
     *
     * @return una lista di tutti i report di gioco nel database
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public List<GameReport> selectAll() {
        return selectBase("SELECT * FROM GameReport");
    }

    /**
     * Recupera tutti i report di gioco che soddisfano una specifica clausola SQL.
     *
     * @param sqlClause la clausola WHERE da applicare (senza la parola chiave WHERE)
     * @param params i parametri da sostituire nella query
     * @return una lista di report di gioco corrispondenti alla clausola specificata
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    public List<GameReport> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM GameReport WHERE " + sqlClause;
        return selectBase(query, params);
    }

    /**
     * Metodo di utilità per eseguire una query e mappare i risultati in oggetti GameReport.
     *
     * @param query la query SQL da eseguire
     * @param params i parametri da sostituire nella query
     * @return una lista di GameReport ottenuti dai risultati della query
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    private List<GameReport> selectBase(String query, Object... params) {
        var result = new ArrayList<GameReport>();
        Callback<ResultSet,List<GameReport>> callback = res -> {
            try {
                if (res == null) {
                    return result;
                }
                while (res.next()) {
                    var user = userDAO.selectBy(res.getString("user"));
                    var docList = executeQuery(
                            "SELECT document FROM Content WHERE report = ?",
                            docs -> {
                                List<Document> documents = new ArrayList<>();
                                try {
                                    while (docs.next()) {
                                        documentDAO.selectBy(docs.getString("document")).ifPresent(documents::add);
                                    }
                                } catch (SQLException e) {
                                    SystemLogger.log("Error trying to get documents", e);
                                    throw new QueryFailedException(e.getMessage());
                                }
                                return documents;
                            },
                            res.getLong("id")
                    );
                    String[] maxParts = res.getString("max_time").split(":");
                    Duration maxTime = Duration.ofMinutes(Long.parseLong(maxParts[0]))
                            .plusSeconds(Long.parseLong(maxParts[1]));
                    String[] usedParts = res.getString("used_time").split(":");
                    Duration usedTime = Duration.ofMinutes(Long.parseLong(usedParts[0]))
                            .plusSeconds(Long.parseLong(usedParts[1]));
                    LocalDateTime timestamp = new Timestamp(res.getLong("timestamp")).toLocalDateTime();
                    if (user.isPresent()) {
                        result.add(new GameReport(
                                user.get(),
                                docList,
                                timestamp,
                                Difficulty.valueOf(res.getString("difficulty")),
                                maxTime,
                                usedTime,
                                res.getInt("question_count"),
                                res.getInt("score")
                        ));
                    }
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all game reports", e);
                throw new QueryFailedException(e.getMessage());
            }
            return result;
        };
        return executeQuery(query, callback, params);
    }

    /**
     * Inserisce un nuovo report di gioco nella tabella GameReport e aggiorna la tabella Content
     * per associare i documenti utilizzati.
     *
     * @param gameReport il report di gioco da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(GameReport gameReport) {
        String insertReport = "INSERT INTO GameReport (user, timestamp, difficulty, max_time, used_time, question_count, score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertContent = "INSERT INTO Content (report, document) VALUES (?, ?)";
        try {
            long usedTotalSeconds = gameReport.usedTime().getSeconds();
            String usedTimeFormatted = preFormatTime(usedTotalSeconds);
            long maxTotalSeconds = gameReport.maxTime().getSeconds();
            String maxTimeFormatted = preFormatTime(maxTotalSeconds);
            long reportId=executeUpdate(insertReport,
                    gameReport.user().getName(),
                    Timestamp.valueOf(gameReport.timestamp()),
                    gameReport.difficulty().name(),
                    maxTimeFormatted,
                    usedTimeFormatted,
                    gameReport.questionCount(),
                    gameReport.score()
            );

            for (Document document : gameReport.documents()) {
                executeUpdate(insertContent, reportId, document.filename());
            }
        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert game report", e);
            throw new QueryFailedException(e.getMessage());
        }
    }
    /**
     * Formatta un valore temporale (in secondi) in una stringa nel formato MM:SS.
     * Garantisce che il tempo formattato rispetti i vincoli del database:
     * @param time il tempo da formattare
     * @return una stringa nel formato "MM:SS" che rappresenta il tempo normalizzato
     */
    private String preFormatTime(long time) {
        long minutes = Math.min(time / 60, 60);
        long seconds = time % 60;
        //Duration contiene frazioni di secondo, che vengono troncate per i vincoli del db
        if (minutes == 60 && seconds > 0) {
            seconds = 0;
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Aggiorna le informazioni di un report di gioco esistente nella tabella GameReport.
     *
     * @param gameReport il report di gioco da aggiornare
     * @throws UpdateFailedException se si verifica un errore durante l'aggiornamento
     */
    @Override
    public void update(GameReport gameReport) {
        String update = "UPDATE GameReport SET user = ?, timestamp = ?, difficulty = ?, max_time = ?, used_time = ?, question_count = ?, score = ? WHERE id = ?";
        try {
            executeUpdate(update,
                    gameReport.user().getName(),
                    gameReport.timestamp(),
                    gameReport.difficulty().name(),
                    gameReport.maxTime().toString(),
                    gameReport.usedTime().toString(),
                    gameReport.questionCount(),
                    gameReport.score()
            );
        } catch (SQLException e) {
            SystemLogger.log("Error trying to update game report", e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Elimina un report di gioco dalla tabella GameReport.
     * L'eliminazione comporta anche la cancellazione dei record associati nella tabella Content
     * grazie ai vincoli di integrità (ON DELETE CASCADE).
     *
     * @param gameReport il report di gioco da eliminare
     * @throws UpdateFailedException se si verifica un errore durante l'eliminazione
     */
    @Override
    public void delete(GameReport gameReport) {
        String updateOnReport = "DELETE FROM GameReport WHERE user = ? AND timestamp = ?";
        try {
            executeUpdate(updateOnReport, gameReport.user().getName(), Timestamp.valueOf(gameReport.timestamp()));
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete game report", e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}