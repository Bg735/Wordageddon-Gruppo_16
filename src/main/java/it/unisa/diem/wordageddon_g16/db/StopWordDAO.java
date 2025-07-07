package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Data Access Object (DAO) per la gestione delle stopword nel database.
 * Permette di eseguire operazioni CRUD (eccetto update e selectById) sulla tabella StopWord.
 * Ogni stopword è rappresentata come una stringa.
 */
public class StopWordDAO extends JdbcDAO<String> {

    /**
     * Costruisce un nuovo StopWordDAO utilizzando la connessione specificata.
     *
     * @param conn la connessione al database da utilizzare per le operazioni
     */
    public StopWordDAO(Connection conn) {
        super(conn);
    }

    /**
     * Operazione non supportata: la selezione per id non è rilevante per le stopword.
     *
     * @param oid identificativo non utilizzato
     * @return mai restituito, viene sempre lanciata un'eccezione
     * @throws UnsupportedOperationException sempre lanciata per questa operazione
     */
    @Override
    public Optional<String> selectById(Object oid) {
        throw new UnsupportedOperationException("This operation is not implemented as it is not useful in this context.");
    }

    /**
     * Recupera tutte le stopword presenti nella tabella StopWord.
     *
     * @return un Set contenente tutte le stopword del database
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
     * Inserisce una nuova stopword nella tabella StopWord.
     *
     * @param s la stopword da inserire
     * @throws QueryFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(String s) {
        String query = "INSERT INTO StopWord (word) VALUES (?)";
        try {
            executeUpdate(query, s);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert stop word: " + s, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Operazione non supportata: l'aggiornamento delle stopword non è previsto.
     *
     * @param s la stopword da aggiornare (non utilizzata)
     * @throws UnsupportedOperationException sempre lanciata per questa operazione
     */
    @Override
    public void update(String s) {
        throw new UnsupportedOperationException("This operation is not implemented as it cannot be used in this context.");
    }

    /**
     * Elimina una stopword dalla tabella StopWord.
     *
     * @param s la stopword da eliminare
     * @throws QueryFailedException se si verifica un errore durante l'eliminazione
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
}
