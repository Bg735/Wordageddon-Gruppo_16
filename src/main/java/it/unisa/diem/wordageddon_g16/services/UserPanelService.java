package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.DAO;
import it.unisa.diem.wordageddon_g16.db.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;

public class UserPanelService {
    private final AppContext context;
    private final UserDAO userDAO;
    private final GameReportDAO gameReportDAO;

    public UserPanelService(AppContext context, UserDAO userDAO, GameReportDAO gameReportDAO) {
        this.context=context;
        this.userDAO = userDAO;
        this.gameReportDAO = gameReportDAO;
    }

    public String getUsername() {
        return context.getCurrentUser().getName();
    }

    public void logout() {
        context.getAuthService().logout();
    }
}
