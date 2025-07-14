package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.db.contracts.StopWordDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.UserDAO;
import it.unisa.diem.wordageddon_g16.db.exceptions.QueryFailedException;
import it.unisa.diem.wordageddon_g16.db.exceptions.UpdateFailedException;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementazione JDBC del {@link UserDAO}, che gestisce le operazioni Dao sullo user.
 * <p>
 * Gli utenti sono salvati nella tabella {@code User}, e rappresentati dal modello {@link User}.
 * Tutte le interazioni con il database sono gestite tramite {@link JdbcDAO}, con logging automatico via {@link SystemLogger}.
 */
public class JDBCUserDAO extends JdbcDAO<User> implements UserDAO {

    /**
     * Costruisce un nuovo {@code JDBCUserDAO} utilizzando la connessione specificata.
     *
     * @param connection la connessione al database da utilizzare per le operazioni
     */
    public JDBCUserDAO(Connection connection) {
        super(connection);
    }

    /**
     * Recupera un utente dal database in base al nome utente.
     *
     * @param username il nome dell’utente da cercare
     * @return un {@code Optional} contenente l’utente se trovato, altrimenti vuoto
     * @throws QueryFailedException se si verifica un errore durante l'esecuzione della query
     */
    @Override
    public Optional<User> selectBy(String username) {
        String query = "SELECT * FROM User WHERE name = ?";
        Callback<ResultSet, Optional<User>> callback = res -> {
            try {
                if (res != null && res.next()) {
                    User user = new User(
                            res.getString("name"),
                            res.getString("password"),
                            res.getBoolean("isAdmin")
                    );
                    return Optional.of(user);
                }
                return Optional.empty();
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get user with id: " + username, e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback, username);
    }

    /**
     * Recupera tutti gli utenti presenti nella tabella {@code User}.
     *
     * @return una lista contenente tutti gli utenti registrati
     * @throws QueryFailedException se si verifica un errore durante la query
     */
    @Override
    public List<User> selectAll() {
        String query = "SELECT * FROM User";
        Callback<ResultSet, List<User>> callback = res -> {
            try {
                var result = new ArrayList<User>();
                if (res == null) {
                    return result;
                }
                while (res.next()) {
                    result.add(new User(
                            res.getString("name"),
                            res.getString("password"),
                            res.getBoolean("isAdmin")
                    ));
                }
                return result;
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all users", e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);
    }
    /**
     * Inserisce un nuovo utente nella tabella {@code User}.
     * <p>
     * Se l’utente esiste già (in base alla chiave primaria), l’inserimento viene ignorato.
     *
     * @param user l’oggetto {@link User} da inserire
     * @throws UpdateFailedException se si verifica un errore durante l'inserimento
     */
    @Override
    public void insert(User user) {
        String query = "INSERT OR IGNORE INTO User (name, password, isAdmin) VALUES (?, ?, ?)";
        try {
            executeUpdate(query, user.getName(), user.getPassword(), user.isAdmin());
        } catch (SQLException e) {
            SystemLogger.log("Error inserting user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Aggiorna le informazioni di un utente esistente.
     *
     * @param user l’oggetto {@link User} da aggiornare
     * @throws UpdateFailedException se si verifica un errore durante l'aggiornamento
     */
    @Override
    public void update(User user) {
        String query = "UPDATE User SET password = ?, isAdmin = ? WHERE name = ?";
        try {
            executeUpdate(query, user.getPassword(), user.isAdmin(), user.getName());
        } catch (SQLException e) {
            SystemLogger.log("Error updating user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Elimina un utente dal database.
     * <p>
     * L'eliminazione comporta anche la cancellazione automatica dei {@code GameReport}
     * associati, grazie ai vincoli di integrità (ON DELETE CASCADE).
     *
     * @param user l’oggetto {@link User} da eliminare
     * @throws UpdateFailedException se si verifica un errore durante la cancellazione
     */
    @Override
    public void delete(User user) {
        String query = "DELETE FROM User WHERE name = ?";
        try {
            executeUpdate(query, user.getName());               // Delete on User also deletes all GameReports associated with the user due to integrity constraints (ON DELETE CASCADE)
        } catch (SQLException e) {
            SystemLogger.log("Error deleting user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    /**
     * Verifica se la tabella {@code User} è vuota.
     *
     * @return {@code true} se non esiste alcun utente nel database, altrimenti {@code false}
     */
    public boolean isEmpty() {
        String query = "SELECT 1 FROM User LIMIT 1";
        try (var stm = connection.createStatement();
             var res = stm.executeQuery(query)) {
            return !res.next();
        } catch (SQLException e) {
            SystemLogger.log("Error checking if User table is empty", e);
            return false;
        }
    }
}