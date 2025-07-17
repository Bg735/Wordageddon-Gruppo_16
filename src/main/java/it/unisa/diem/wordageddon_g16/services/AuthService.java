package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCUserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.utility.Config;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.io.*;

/**
 * Classe di 'servizio' utilizzata per l'autenticazione e la gestione degli utenti nell'applicazione Wordageddon.
 * <p>
 * Permette la registrazione, login, logout e gestione della sessione utente.
 * Utilizza {@link JDBCUserDAO} per la persistenza e {@link AppContext} per il tracciamento dell'utente corrente.
 */
public class AuthService implements Serializable {
    private final AppContext context;
    private final JDBCUserDAO userDAO;

    /**
     * Costruttore della classe {@code AuthService}
     *
     * @param context  AppContent contesto applicativo corrente contenente l'utente attivo
     * @param userDAO  DAO per la gestione degli utenti
     */
    public AuthService(AppContext context, JDBCUserDAO userDAO) {
        this.context = context;
        this.userDAO = userDAO;
    }

    /**
     * Effettua il login utente verificando le credenziali fornite.
     * <p>
     * Se le credenziali sono valide:
     * <ul>
     *   <li>Imposta l'utente corrente nel {@link AppContext}</li>
     *   <li>Salva la sessione localmente</li>
     * </ul>
     *
     * @param username nome utente
     * @param password password associata
     * @return {@code true} se il login ha successo, {@code false} altrimenti
     */
    public boolean login(String username, String password) {
        var user = userDAO.selectBy(username);
        if(user.isPresent() && user.get().getPassword().equals(password)) {
            context.setCurrentUser(user.get());
            saveSession(user.get());
            return true;
        }
        return false; // User not found or password mismatch
    }

    /**
     * Registra un nuovo utente con nome e password specificati.
     * <p>
     * Se è il primo utente, viene creato come amministratore.
     * <br>
     * Salva automaticamente la sessione se la registrazione va a buon fine.
     *
     * @param username  nome utente da registrare
     * @param password  password associata
     * @param firstUser {@code true} se è il primo utente in assoluto
     * @return {@code true} se la registrazione ha successo, {@code false} se l'utente esiste già
     */
    public boolean register(String username, String password, boolean firstUser) {
        if(firstUser || userDAO.selectBy(username).isEmpty()) {
            User user = new User(username, password, firstUser); // If no users, set as admin
            userDAO.insert(user);
            context.setCurrentUser(user);
            saveSession(user);
            return true;
        }
        return false; // User already exists
    }

    /**
     * Salva l'oggetto {@link User} corrente in un file locale come sessione attiva.
     *
     * @param user utente da salvare come sessione
     */
    private void saveSession(User user) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Config.get(Config.Props.SESSION_FILE)))) {
            out.writeObject(user);
        } catch (IOException e) {
            SystemLogger.log("Errore nel salvataggio della sessione", e);
        }
    }

    /**
     * Carica la sessione utente precedentemente salvata dal file locale.
     * <p>
     * Se il file esiste e contiene un utente valido:
     * <ul>
     *   <li>Imposta l'utente corrente nel {@link AppContext}</li>
     *   <li>Ritorna {@code true}</li>
     * </ul>
     *
     * @return {@code true} se la sessione è stata caricata con successo, {@code false} altrimenti
     */
    public boolean loadSession() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(Config.get(Config.Props.SESSION_FILE)))) {
            User user = (User) in.readObject();
            if (user != null) {
                context.setCurrentUser(user);
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            SystemLogger.log("Errore nel caricamento della sessione", e);
        }
        return false;
    }

    /**
     * Esegue il logout dell'utente corrente.
     * <p>
     * Rimuove l'utente dal {@link AppContext} e cancella il file di sessione.
     * <br>
     * Se la cancellazione del file fallisce, viene sollevata una {@link RuntimeException}.
     * </p>
     */
    public void logout() {
        context.setCurrentUser(null);
        if (!new File(Config.get(Config.Props.SESSION_FILE)).delete()) {
            var e = new IOException("Impossibile cancellare il file di sessione");
            SystemLogger.log("Errore nella cancellazione del file di sessione", e);
            throw new RuntimeException("Errore nella cancellazione del file di sessione", e);
        }
    }

    /**
     * Verifica se non ci sono utenti registrati nel sistema.
     *
     * @return {@code true} se il database utenti è vuoto
     */
    public boolean noUsers(){
        return userDAO.isEmpty();
    }

}
