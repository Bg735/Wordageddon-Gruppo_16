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
 * Classe astratta base per l'accesso ai dati tramite JDBC.
 * <p>
 * Implementa il pattern DAO e fornisce metodi comuni per eseguire query SQL parametrizzate,
 * operazioni di aggiornamento e verifiche strutturali. Ogni DAO specifico estende questa classe.
 *
 * @param <T> tipo dell'entità gestita dal DAO
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
     * Esegue una query SQL con parametri e applica una {@link Callback} per elaborare il {@link ResultSet}.
     *
     * @param sql     query SQL parametrizzata
     * @param cb      callback che processa il risultato della query
     * @param params  parametri da sostituire nella query
     * @param <R>     tipo di dato restituito dalla callback
     * @return risultato ottenuto dalla callback
     * @throws QueryFailedException se la query fallisce durante l'esecuzione
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
     * Esegue una query SQL semplice (senza parametri) e ne elabora il risultato tramite {@link Callback}.
     *
     * @param sql query SQL da eseguire
     * @param cb  callback che processa il {@link ResultSet}
     * @param <R> tipo di risultato prodotto dalla callback
     * @return risultato ottenuto dalla callback
     * @throws QueryFailedException se la query fallisce
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
     * Esegue un'operazione di modifica (INSERT, UPDATE, DELETE) sul database.
     * <p>
     * Se l'operazione è una INSERT, restituisce l'ID generato dalla riga appena inserita.
     *
     * @param sql    istruzione SQL da eseguire
     * @param params parametri per l'inserimento
     * @return ID generato, oppure {@code -1} se assente
     * @throws SQLException se l'esecuzione fallisce
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
     * Verifica se una tabella contiene almeno una riga.
     *
     * @param tableName nome della tabella da controllare
     * @return {@code true} se contiene righe, {@code false} se è vuota o se la query fallisce
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