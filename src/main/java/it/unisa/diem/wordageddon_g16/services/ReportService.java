package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.contracts.GameReportDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;

public class ReportService {
    private final AppContext appContext;
    private final GameReportDAO gameReportDAO;

    public ReportService(AppContext appContext, GameReportDAO gameReportDAO) {
        this.appContext = appContext;
        this.gameReportDAO = gameReportDAO;
    }
}