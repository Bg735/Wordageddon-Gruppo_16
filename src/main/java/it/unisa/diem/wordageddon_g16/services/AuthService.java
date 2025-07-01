package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.User;

import java.io.*;

public class AuthService {

    private final AppContext context;
    private final UserDAO userDAO;
    ;

    public AuthService(AppContext context, UserDAO userDAO) {
        this.context = context;
        this.userDAO = userDAO;
    }

    public boolean login(String username, String password) {
        var user = userDAO.selectById(username);
        if(user.isPresent() && user.get().getPassword().equals(password)) {
            context.setCurrentUser(user.get());
            saveSession(user.get());
            return true;
        }
        return false; // User not found or password mismatch
    }

    public boolean register(String username, String password, boolean firstUser) {
        if(firstUser || userDAO.selectById(username).isEmpty()) {
            User user = new User(username, password, firstUser); // If no users, set as admin
            userDAO.insert(user);
            context.setCurrentUser(user);
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
    public boolean restoreSession() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(Config.get(Config.Props.SESSION_FILE)))) {
            User user = (User) in.readObject();
            if (user != null) {
                context.setCurrentUser(user);
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {}
        return false;
    }

    public void logout() {
        context.setCurrentUser(null);
        new File(Config.get(Config.Props.SESSION_FILE)).delete();
    }

    public boolean noUsers(){
        return userDAO.isEmpty();
    }

}
