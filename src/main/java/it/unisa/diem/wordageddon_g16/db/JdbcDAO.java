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
 * Classe astratta di supporto per la realizzazione di DAO (Data Access Object) basati su JDBC.
 *
 * <p>
 * Fornisce un insieme di metodi protetti e riutilizzabili per semplificare l'accesso al database,
 * centralizzando la logica comune di esecuzione di query, aggiornamenti e gestione delle risorse.
 * Ciascun DAO concreto dovrà estendere questa classe e implementare i metodi specifici previsti
 * dall'interfaccia {@link DAO}.
 * </p>
 * <p>
 * <p>
 * Caratteristiche principali:
 * <ul>
 *   <li>Gestione automatica di {@code PreparedStatement} e {@code ResultSet} tramite try-with-resources.</li>
 *   <li>Supporto a query parametrizzate e non, tramite metodi generici che sfruttano una {@link Callback}
 *       per l’elaborazione flessibile dei risultati dalla query SQL (ResultSet).</li>
 *   <li>Gestione centralizzata delle eccezioni e logging automatico in caso di errore.</li>
 *   <li>Metodi utility per operazioni semplici e frequenti (es. verifica se una tabella è vuota).</li>
 * </ul>
 *
 * @param <T> tipo dell'entità gestita dal DAO concreto
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
     * @param <R>    tipo di dato restituito dalla callback
     * @param sql    query SQL parametrizzata
     * @param cb     callback che processa il risultato della query
     * @param params parametri da sostituire nella query
     * @return risultato ottenuto dalla callback
     * @throws QueryFailedException se la query fallisce durante l'esecuzione
     */
    protected <R> R executeQuery(String sql, Callback<ResultSet, R> cb, Object... params) {
        // Callback consente di passare un metodo come parametro per elaborare il ResultSet
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
     * @param <R> tipo di risultato prodotto dalla callback
     * @param sql query SQL da eseguire
     * @param cb  callback che processa il {@link ResultSet}
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