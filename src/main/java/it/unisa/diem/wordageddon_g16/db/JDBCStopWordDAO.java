package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DocumentDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implementazione JDBC del {@link StopWordDAO}, che gestisce le operazioni sulle stopwords.
 * <p>
 * Le stopwords sono salvati nella tabella {@code StopWord}.
 * Tutte le interazioni con il database sono gestite tramite {@link JdbcDAO}, con logging automatico via {@link SystemLogger}.
 */
public class JDBCStopWordDAO extends JdbcDAO<String> implements StopWordDAO {

    /**
     * Costruisce un nuovo {@code JDBCStopWordDAO} utilizzando la connessione specificata.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     */
    public JDBCStopWordDAO(Connection conn) {
        super(conn);
    }

    /**
     * Recupera tutte le stopword presenti nel database.
     *
     * @return un insieme di stringhe contenente tutte le stopword registrate
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public Set<String> selectAll() {
        String query = "SELECT word FROM StopWord";
        Callback<ResultSet,Set<String>> callback = res -> {
            try{
                Set<String> stopWords = new HashSet<>();
                while (res.next()) {
                    stopWords.add(res.getString("word"));
                }
                return stopWords;
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all stop words", e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);
    }

    /**
     * Inserisce una nuova stopword nel database.
     * <p>
     * Se la parola è già presente, non viene eseguita alcuna operazione grazie all'uso di {@code INSERT OR IGNORE}.
     *
     * @param s la stopword da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(String s) {
        String query = "INSERT OR IGNORE INTO StopWord (word) VALUES (?)";
        try {
            executeUpdate(query, s);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert stop word: " + s, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Operazione non supportata: l'aggiornamento di una stopword non è previsto.
     *
     * @param s la stopword da aggiornare (non utilizzata)
     * @throws UnsupportedOperationException sempre sollevata, perché l'operazione è disabilitata
     */
    @Override
    public void update(String s) {
        throw new UnsupportedOperationException("This operation is not implemented as it cannot be used in this context.");
    }

    /**
     * Elimina una stopword specifica dal database.
     *
     * @param s la stopword da eliminare
     * @throws QueryFailedException se si verifica un errore durante la cancellazione
     */
    @Override
    public void delete(String s) {
        String query = "DELETE FROM StopWord WHERE word = ?";
        try {
            executeUpdate(query, s);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete stop word: " + s, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Verifica se la tabella {@code StopWord} è vuota.
     *
     * @return {@code true} se la tabella non contiene alcuna stopword, {@code false} altrimenti
     */
    @Override
    public boolean isEmpty() {
        return super.isEmpty("StopWord");
    }
}