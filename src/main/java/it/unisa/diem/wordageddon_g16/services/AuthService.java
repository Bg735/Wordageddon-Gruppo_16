package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCUserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.utility.Config;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.io.*;

public class AuthService {

    private final AppContext context;
    private final JDBCUserDAO userDAO;


    public AuthService(AppContext context, JDBCUserDAO userDAO) {
        this.context = context;
        this.userDAO = userDAO;
    }

    public boolean login(String username, String password) {
        var user = userDAO.selectBy(username);
        if(user.isPresent() && user.get().getPassword().equals(password)) {
            context.setCurrentUser(user.get());
            saveSession(user.get());
            return true;
        }
        return false; // User not found or password mismatch
    }

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
    private void saveSession(User user) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Config.get(Config.Props.SESSION_FILE)))) {
            out.writeObject(user);
        } catch (IOException e) {
            SystemLogger.log("Errore nel salvataggio della sessione", e);
        }
    }
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

    public void logout() {
        context.setCurrentUser(null);
        if (!new File(Config.get(Config.Props.SESSION_FILE)).delete()) {
            var e = new IOException("Impossibile cancellare il file di sessione");
            SystemLogger.log("Errore nella cancellazione del file di sessione", e);
            throw new RuntimeException("Errore nella cancellazione del file di sessione", e);
        }
    }

    public boolean noUsers(){
        return userDAO.isEmpty();
    }

}
