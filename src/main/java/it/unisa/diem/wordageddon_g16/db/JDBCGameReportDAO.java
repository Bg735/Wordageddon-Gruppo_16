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
 * Implementazione JDBC del {@link DocumentDAO}, che gestisce le operazioni sui report.
 * <p>
 * I report sono salvati nella tabella {@code GameReport} e rappresentati tramite il model {@link GameReport}.
 * Tutte le interazioni con il database sono gestite tramite {@link JdbcDAO}, con logging automatico via {@link SystemLogger}.
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
     * Costruisce un nuovo {@code JDBCGameReportDAO} utilizzando la connessione e i DAO specificati.
     *
     * @param conn        la connessione al database da utilizzare per le operazioni
     * @param documentDAO il DAO per la gestione dei documenti
     * @param userDAO     il DAO per la gestione degli utenti
     */
    public JDBCGameReportDAO(Connection conn, DAO<Document> documentDAO, DAO<User> userDAO) {
        super(conn);
        this.userDAO = (UserDAO) userDAO;
        this.documentDAO = (DocumentDAO) documentDAO;
    }

    /**
     * Recupera un report di gioco dal database sulla base dell'utente e del timestamp.
     *
     * @param user      l'utente autore del report
     * @param timestamp il timestamp univoco della partita
     * @return un {@code Optional} contenente il report trovato, o vuoto se non esiste
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public Optional<GameReport> selectBy(User user, Timestamp timestamp) {
        return selectWhere("user = ? AND timestamp = ?", user, timestamp).stream().findFirst();
    }

    /**
     * Recupera tutti i report di gioco presenti nella tabella {@code GameReport}.
     *
     * @return una lista di tutti i report presenti nel database
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public List<GameReport> selectAll() {
        return selectBase("SELECT * FROM GameReport");
    }

    /**
     * Recupera i report di gioco che soddisfano una specifica clausola SQL.
     *
     * @param sqlClause la clausola {@code WHERE} da applicare (senza la parola chiave {@code WHERE})
     * @param params    i parametri da sostituire nella query
     * @return una lista di report che corrispondono ai criteri specificati
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    public List<GameReport> selectWhere(String sqlClause, Object... params) {
        String query = "SELECT * FROM GameReport WHERE " + sqlClause;
        return selectBase(query, params);
    }

    /**
     * Metodo interno di utilità per eseguire una query di selezione e mappare i risultati in oggetti {@link GameReport}.
     * Recupera anche i documenti associati tramite la tabella {@code Content}.
     *
     * @param query  la query SQL completa da eseguire
     * @param params i parametri da usare nella query
     * @return una lista di {@link GameReport} risultanti
     * @throws QueryFailedException se si verifica un errore durante l'elaborazione dei risultati
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
     * Inserisce un nuovo {@link GameReport} nel database e associa i documenti
     * tramite la tabella {@code Content}.
     *
     * @param gameReport il report da salvare
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
     * @return una {@code String} nel formato "MM:SS" che rappresenta il tempo normalizzato
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
     * Aggiorna un {@link GameReport} esistente nel database.
     * <p>
     * Nota: questa operazione modifica solo la tabella {@code GameReport},
     * non la tabella {@code Content}.
     *
     * @param gameReport il report aggiornato
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
     * Elimina un {@link GameReport} dal database.
     * <p>
     * Grazie al vincolo {@code ON DELETE CASCADE}, i documenti associati nella
     * tabella {@code Content} vengono eliminati automaticamente.
     *
     * @param gameReport il report da eliminare
     * @throws UpdateFailedException se si verifica un errore durante la rimozione
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