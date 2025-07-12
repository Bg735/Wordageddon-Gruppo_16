//package it.unisa.diem.wordageddon_g16.services;
//
//import it.unisa.diem.wordageddon_g16.db.contracts.GameReportDAO;
//import it.unisa.diem.wordageddon_g16.models.AppContext;
//import it.unisa.diem.wordageddon_g16.models.GameReport;
//
//public class ReportService {
//    private final AppContext appContext;
//    private final GameReportDAO gameReportDAO;
//    private final GameReport gameReport;
//
//    public ReportService(AppContext appContext, GameReportDAO gameReportDAO) {
//        this.appContext = appContext;
//        this.gameReportDAO = gameReportDAO;
//        gameReport = appContext.getCurrentGameReport();
//    }
//
//    public GameReport getGameReport() {
//        if (gameReport == null) {
//            throw new IllegalStateException("Game report is not set in the application context.");
//        }
//        return gameReport;
//    }
//
//}