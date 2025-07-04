package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.AuthService;
import it.unisa.diem.wordageddon_g16.services.LeaderboardService;
import it.unisa.diem.wordageddon_g16.services.UserPanelService;

public class AppContext {
    private final AuthService authService;
    private final LeaderboardService leaderboardService;
    //private final UserPanelService userPanelService;

    private User currentUser;

    public AppContext(Repository repo) {
        authService = new AuthService(this,repo.getDAO("user"));
        leaderboardService = new LeaderboardService(this, repo.getDAO("gameReport"), repo.getDAO("user"));
        //userPanelService = new UserPanelService(this, repo.getDAO("user"), repo.getDAO("gameReport"));
    }

    public AuthService getAuthService() { return authService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
   // public UserPanelService getUserPanelService() {return userPanelService; }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}
