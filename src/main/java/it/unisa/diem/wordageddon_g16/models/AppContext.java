package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.*;


public class AppContext {
    private final Repository repo;

    private final AuthService authService;
    private final LeaderboardService leaderboardService;
    public final UserPanelService userPanelService;
    private final GameService gameService;
    private final ReportService reportService;

    private User currentUser;

    public AppContext(Repository repo) {
        this.repo = repo;
        authService = new AuthService(this, repo.getDAO("user"));
        leaderboardService = new LeaderboardService(this, repo.getDAO("gameReport"), repo.getDAO("user"));
        userPanelService = new UserPanelService(repo.getDAO("gameReport"), repo.getDAO("user"), repo.getDAO("document"), repo.getDAO("stopWord"), repo.getDAO("wdm"), this);
        gameService = new GameService(this, repo.getDAO("gameReport"), repo.getDAO("wdm"), repo.getDAO("document"), repo.getDAO("stopWord"));
        reportService= new ReportService(this, repo.getDAO("gameReport"));
    }

    public AuthService getAuthService() { return authService; }
    public LeaderboardService getLeaderboardService() { return leaderboardService; }
    public UserPanelService getUserPanelService() {return userPanelService; }
    public GameService getGameService() { return gameService; }
    public User getCurrentUser() {
        return currentUser;
    }
    public Repository getRepo() {
        return repo;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}