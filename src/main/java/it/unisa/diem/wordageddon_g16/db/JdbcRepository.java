package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.DAO;
import it.unisa.diem.wordageddon_g16.db.contracts.Repository;
import it.unisa.diem.wordageddon_g16.utility.Config;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Implementazione della interfaccia {@link Repository} che gestisce l'accesso ai dati tramite JDBC.
 * <p>
 * Inizializza le DAO necessarie al funzionamento del sistema e mantiene una connessione persistente
 * al database definito in {@link Config.Props#DB_URL}. Abilita le chiavi esterne per SQLite.
 */
public class JdbcRepository implements Repository {
    private final Map<String, JdbcDAO<?>> daos = new HashMap<>();
    private Connection conn;

    /**
     * Costruisce un {@code JdbcRepository} e stabilisce una connessione al database.
     * <p>
     * Configura i DAO per le entità:
     * <ul>
     *   <li>{@code user} – {@link JDBCUserDAO}</li>
     *   <li>{@code document} – {@link JDBCDocumentDAO}</li>
     *   <li>{@code stopWord} – {@link JDBCStopWordDAO}</li>
     *   <li>{@code gameReport} – {@link JDBCGameReportDAO}</li>
     *   <li>{@code wdm} – {@link JDBCWdmDAO}</li>
     * </ul>
     * Abilita le foreign key con {@code PRAGMA foreign_keys = ON}.
     * In caso di errore, registra l'evento tramite {@link SystemLogger}.
     */
    public JdbcRepository() {
        try {
            conn = DriverManager.getConnection(Config.get(Config.Props.DB_URL));
            // Abilita le foreign key per la connessione SQLite
            try (var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            var userDAO = new JDBCUserDAO(conn);
            var documentDAO = new JDBCDocumentDAO(conn);
            daos.put("user", userDAO);
            daos.put("document", documentDAO);
            daos.put("stopWord", new JDBCStopWordDAO(conn));
            daos.put("gameReport", new JDBCGameReportDAO(conn, documentDAO, userDAO));
            daos.put("wdm", new JDBCWdmDAO(conn, documentDAO));
        } catch (SQLException e) {
            SystemLogger.log("Could not establish a connection to the database: ", e);
        }
    }

    /**
     * Restituisce il DAO associato a una specifica categoria testuale.
     * <p>
     * La categoria deve corrispondere a una chiave registrata in fase di costruzione
     * (es. {@code "user"}, {@code "document"}, ecc.). Il ritorno è tipizzato tramite generics.
     *
     * @param category stringa identificativa della DAO richiesta
     * @param <T> tipo di entità gestita dalla DAO
     * @param <TDAO> tipo di DAO che estende {@link DAO}
     * @return istanza del DAO associato alla categoria
     * @throws IllegalArgumentException se la categoria non è valida
     */
    @SuppressWarnings("unchecked")
    public <T,TDAO extends DAO<T>> TDAO getDAO(String category) {
        if (daos.containsKey(category)) {
            return (TDAO) daos.get(category);
        } else {
            throw new IllegalArgumentException("No DAO found for category: " + category);
        }
    }

    /**
     * Chiude la connessione aperta al database, se presente.
     * <p>
     * In caso di errore durante la chiusura, viene registrato tramite {@link SystemLogger}.
     */
    public void close(){
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                SystemLogger.log("Could not close the database connection: ", e);
            }
        }
    }
}