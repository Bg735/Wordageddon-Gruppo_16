package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.db.contracts.Repository;
import it.unisa.diem.wordageddon_g16.services.*;

import java.io.Serializable;

/**
 * Classe che rappresenta il contesto dell'applicazione.
 * <p>
 * Fornisce un punto di accesso condiviso a tutti le classi service e DAO, mantenendo anche
 * lo stato globale dell'app, come l'utente corrente e il report di gioco attivo.
 */
public class AppContext implements Serializable {
    /**
     * Repository contenente tutti i DAO per l'accesso al database.
     */
    private final Repository repo;
    /**
     * AuthService per l'autenticazione e la gestione degli utenti.
     */
    private final AuthService authService;
    /**
     * Servizio per la gestione della leaderboard.
     */
    private final LeaderboardService leaderboardService;
    /**
     * Servizio per la gestione del pannello utente.
     */
    public final UserPanelService userPanelService;
    /**
     * Servizio per la logica di gioco, gestione domande e risultati.
     */
    private final GameService gameService;

    /**
     * Utente attualmente autenticato nel sistema.
     */
    private User currentUser;

    /**
     * Sessione di gioco interrotta, utile per riprendere il gioco in caso di interruzione.
     */
    private GameSessionState interruptedSession;

    /**
     * Costruisce un nuovo contesto dell'applicazione inizializzando tutte le classi service
     * con i rispettivi DAO dal repository.
     *
     * @param repo il repository contenente i DAO per l'accesso al database
     */
    public AppContext(Repository repo) {
        this.repo = repo;
        authService = new AuthService(this, repo.getDAO("user"));
        leaderboardService = new LeaderboardService(this, repo.getDAO("gameReport"), repo.getDAO("user"));
        userPanelService = new UserPanelService(repo.getDAO("gameReport"), repo.getDAO("user"), repo.getDAO("document"), repo.getDAO("stopWord"), repo.getDAO("wdm"), this);
        gameService = new GameService(this, repo.getDAO("gameReport"), repo.getDAO("wdm"), repo.getDAO("document"), repo.getDAO("stopWord"));
    }

    /**
     * Restituisce la sessione di gioco interrotta, se presente.
     *
     * @return la sessione di gioco interrotta
     */
    public GameSessionState getInterruptedSession() {
        return interruptedSession;
    }

    /**
     * Imposta la sessione di gioco interrotta.
     * <p>
     * Questa sessione può essere utilizzata per riprendere il gioco in caso di interruzione.
     *
     * @param interruptedSession la sessione di gioco da impostare come interrotta
     */
    public void setInterruptedSession(GameSessionState interruptedSession) {
        this.interruptedSession = interruptedSession;
    }

    /**
     * Restituisce il servizio di autenticazione.
     *
     * @return il servizio {@link AuthService}
     */
    public AuthService getAuthService() {
        return authService;
    }

    /**
     * Restituisce il servizio della leaderboard.
     *
     * @return il servizio {@link LeaderboardService}
     */
    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    /**
     * Restituisce il servizio per la gestione del pannello utente.
     *
     * @return il servizio {@link UserPanelService}
     */
    public UserPanelService getUserPanelService() {
        return userPanelService;
    }

    /**
     * Restituisce il servizio di gioco.
     *
     * @return il servizio {@link GameService}
     */
    public GameService getGameService() {
        return gameService;
    }

    /**
     * Restituisce l'utente attualmente autenticato nel sistema.
     *
     * @return l'utente corrente, o {@code null} se nessuno è loggato
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Imposta l'utente attualmente autenticato nel sistema.
     *
     * @param currentUser l'utente da impostare come corrente
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Restituisce il repository contenente tutti i DAO.
     *
     * @return il repository in uso
     */
    public Repository getRepo() {
        return repo;
    }
}