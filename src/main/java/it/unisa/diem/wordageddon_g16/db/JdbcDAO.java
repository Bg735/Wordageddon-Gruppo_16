package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe astratta che implementa il pattern Data Access Object (DAO) per la gestione
 * delle operazioni di accesso al database tramite JDBC.
 * Fornisce metodi di utilità per l'esecuzione di query e aggiornamenti parametrizzati.
 *
 * @param <T> il tipo di oggetto gestito dal DAO
 */
public abstract class JdbcDAO<T> implements DAO<T> {

    /**
     * Connessione persistente al database utilizzata dal DAO.
     */
    protected final Connection connection;

    /**
     * Costruisce un nuovo JdbcDAO utilizzando la connessione specificata.
     *
     * @param connection la connessione al database
     */
    protected JdbcDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Esegue una query parametrizzata sul database e applica una callback per processare il ResultSet.
     *
     * @param sql la query SQL da eseguire
     * @param cb la callback per processare il ResultSet
     * @param params i parametri da sostituire nella query
     * @param <R> il tipo di risultato restituito dalla callback
     * @return il risultato elaborato dalla callback
     * @throws QueryFailedException se si verifica un errore durante l'esecuzione della query
     */
    protected <R> R executeQuery(String sql, Callback<ResultSet, R> cb, Object... params) {
        try (var stm = connection.prepareStatement(sql)) {
            if (params.length > 0)
                for (int i = 0; i < params.length; i++)
                    stm.setObject(i + 1, params[i]);
            return cb.call(stm.executeQuery());
        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Esegue una query senza parametri sul database e applica una callback per processare il ResultSet.
     *
     * @param sql la query SQL da eseguire
     * @param cb la callback per processare il ResultSet
     * @param <R> il tipo di risultato restituito dalla callback
     * @return il risultato elaborato dalla callback
     * @throws QueryFailedException se si verifica un errore durante l'esecuzione della query
     */
    protected <R> R executeQuery(String sql, Callback<ResultSet, R> cb) {
        try (var stm = connection.createStatement()) {
            return cb.call(stm.executeQuery(sql));
        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    /**
     * Esegue un'operazione di aggiornamento (INSERT, UPDATE, DELETE) sul database con parametri.
     *
     * @param sql la query SQL da eseguire
     * @param params i parametri da sostituire nella query
     * @throws SQLException se si verifica un errore durante l'esecuzione dell'update
     * @return l'ID generato per la riga inserita, se l'operazione è un INSERT e ne ha uno.
     */
    protected long executeUpdate(String sql, Object... params) throws SQLException {
        try (var stm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (params.length > 0)
                for (int i = 0; i < params.length; i++)
                    stm.setObject(i + 1, params[i]);
            stm.executeUpdate();
            return stm.getGeneratedKeys().next() ? stm.getGeneratedKeys().getLong(1) : -1;
        }
    }

    /**
     * Verifica se una tabella del database è vuota.
     *
     * @param tableName il nome della tabella da controllare
     * @return true se la tabella contiene almeno una riga, false altrimenti
     */

    protected boolean isEmpty(String tableName) {
        String query = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (var stm = connection.createStatement();
             var res = stm.executeQuery(query)) {
            return res.next();
        } catch (SQLException e) {
            SystemLogger.log("Error checking if " + tableName + "has any rows", e);
            return false;
        }
    }
}