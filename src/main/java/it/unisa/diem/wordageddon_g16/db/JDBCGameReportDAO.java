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
     * @param id l'identificativo del report da recuperare
     * @return un Optional contenente il report trovato, o vuoto se non esiste
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public Optional<GameReport> selectById(Object id) {
        return selectWhere("id = ?", id).stream().findFirst();
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
                    var user = userDAO.selectById(res.getString("user"));
                    var docList = executeQuery(
                            "SELECT document FROM Content WHERE report = ?",
                            docs -> {
                                List<Document> documents = new ArrayList<>();
                                try {
                                    while (docs.next()) {
                                        documentDAO.selectById(docs.getString("document")).ifPresent(documents::add);
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
                    LocalDateTime timestamp = LocalDateTime.parse(res.getString("timestamp"));
                    if (user.isPresent()) {
                        result.add(new GameReport(
                                res.getLong("id"),
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
            // Conversione diretta delle Duration in formato "HH:MM"
            String maxTimeFormatted = String.format("%02d:%02d",
                    gameReport.maxTime().toMinutes() / 60,
                    gameReport.maxTime().toMinutes() % 60
            );
            String usedTimeFormatted = String.format("%02d:%02d",
                    gameReport.usedTime().toMinutes() / 60,
                    gameReport.usedTime().toMinutes() % 60
            );

            long reportId = executeUpdateAndReturnGeneratedKey(
                    insertReport,
                    gameReport.user().getName(),
                    gameReport.timestamp(),
                    gameReport.difficulty().name(),
                    maxTimeFormatted,      // Formattato come "HH:MM"
                    usedTimeFormatted,     // Formattato come "HH:MM"
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
     *  METODO TEMPORANEO, CONTROLLARE
     * Esegue una query di INSERT e restituisce la chiave generata (ad esempio l'id autoincrement).
     *
     * @param sql la query di INSERT (con eventuali segnaposto ?)
     * @param params i parametri da inserire nei segnaposto
     * @return il valore della chiave generata (tipicamente l'id)
     * @throws SQLException se qualcosa va storto nell'esecuzione
     */
    private long executeUpdateAndReturnGeneratedKey(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Imposta i parametri nella query
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            // Recupera la chiave generata
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new SQLException("Nessuna chiave generata dal database.");
                }
            }
        }
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
                    gameReport.score(),
                    gameReport.id()
            );
        } catch (SQLException e) {
            SystemLogger.log("Error trying to update game report with id: " + gameReport.id(), e);
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
        String updateOnReport = "DELETE FROM GameReport WHERE id = ?";
        try {
            executeUpdate(updateOnReport, gameReport.id());
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete game report with id: " + gameReport.id(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}