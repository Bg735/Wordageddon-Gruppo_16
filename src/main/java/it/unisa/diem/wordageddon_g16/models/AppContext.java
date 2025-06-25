package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.AuthService;

public class AppContext {
    private final AuthService authService;

    private User currentUser;

    public AppContext(Repository repo) {
        authService = new AuthService(this,repo.getDAO("user"));

    }

    public AuthService getAuthService() {
        return authService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
