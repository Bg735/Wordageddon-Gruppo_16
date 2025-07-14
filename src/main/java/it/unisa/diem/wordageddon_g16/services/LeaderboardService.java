package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.JDBCGameReportDAO;
import it.unisa.diem.wordageddon_g16.db.contracts.UserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe che gestisce la classifica globale e filtrata per difficoltà nel gioco.
 * <p>
 * Recupera i dati dei punteggi dei giocatori tramite {@link JDBCGameReportDAO} e costruisce
 * statistiche aggregate come punteggio medio, totale, e numero di partite giocate.
 * Evidenzia l'utente corrente nella lista e determina la difficoltà preferita se non filtrata.
 */
public class LeaderboardService {
    /**
     * Record interno che rappresenta una voce nella classifica.
     * <p>
     * Contiene il nome utente, la difficoltà preferita (se nota), il punteggio medio,
     * il punteggio totale accumulato e il numero di partite giocate.
     */
    public record LeaderboardEntry(
        String username,
        Difficulty favouriteDifficulty, // nullable
        int averageScore,
        int totalScore,
        int gamesPlayed
    ){}
    private final JDBCGameReportDAO gameReportDAO;
    private final Collection<User> users;
    private final User currentUser;

    /**
     * Cosstruttore della classe {@code LeaderboardService}.
     *
     * @param context       il contesto applicativo corrente contenente l'utente attivo
     * @param gameReportDAO DAO per i report di gioco
     * @param userDAO       DAO per la selezione degli utenti
     */
    public LeaderboardService(AppContext context, JDBCGameReportDAO gameReportDAO, UserDAO userDAO) {
        this.currentUser = context.getCurrentUser();
        this.gameReportDAO = gameReportDAO;
        this.users = userDAO.selectAll();
    }

    /**
     * Restituisce la classifica globale di tutti gli utenti, indipendentemente dalla difficoltà.
     * <p>
     * Ogni voce include punteggio medio, totale e statistiche aggregate.
     * </p>
     *
     * @return lista ordinata di {@link LeaderboardEntry} per tutti gli utenti
     */
    public List<LeaderboardEntry> getGloablLeaderboard() {
        return getLeaderboardBase(null);
    }

    /**
     * Restituisce la classifica filtrata per una specifica difficoltà.
     *
     * @param difficulty difficoltà da considerare per la classifica
     * @return lista ordinata di {@link LeaderboardEntry} relativa alla difficoltà selezionata
     * @throws IllegalArgumentException se {@code difficulty} è {@code null}
     */
    public List<LeaderboardEntry> getLeaderboardByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty cannot be null");
        }
        return getLeaderboardBase(difficulty);
    }
    /**
     * Metodo interno che costruisce la classifica base, utilizzata sia globalmente che per singola difficoltà.
     * <p>
     * Per ogni utente:
     * <ul>
     *   <li>Recupera i report di gioco filtrati (se richiesto)</li>
     *   <li>Calcola media, totale e numero di partite</li>
     *   <li>Determina la difficoltà preferita se non filtrato</li>
     *   <li>Evidenzia l'utente corrente apponendo "(Tu)"</li>
     * </ul>
     *
     * @param difficulty difficoltà da filtrare, {@code null} per classifica globale
     * @return lista ordinata di {@link LeaderboardEntry}
     */
    private List<LeaderboardEntry> getLeaderboardBase(Difficulty difficulty) {
        var result = new ArrayList<LeaderboardEntry>();
        for (User user : users) {
            Object[] params;
            if (difficulty != null)
                params = new Object[]{user.getName(), difficulty.name()};
            else
                params = new Object[]{user.getName()};

            var reports = gameReportDAO.selectWhere(
                    "user = ?" + (difficulty != null ? " AND difficulty = ?" : ""),
                    params
            );

            int totalScore;
            int gamesPlayed;
            int averageScore;

            if (reports.isEmpty()){
                totalScore = 0;
                gamesPlayed = 0;
                averageScore = 0;
            }
            else {
                totalScore = reports.stream().mapToInt(GameReport::score).sum();
                gamesPlayed = reports.size();
                averageScore = totalScore / gamesPlayed;
            }
            Difficulty favouriteDifficulty = null;
            if(difficulty==null)
                favouriteDifficulty = reports.stream()
                    .map(GameReport::difficulty)
                    .collect(Collectors.groupingBy(d -> d,Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

            var username = user.equals(currentUser) ? user.getName() + " (Tu)" : user.getName();
            result.add(new LeaderboardEntry(
                    username,
                    favouriteDifficulty,
                    averageScore,
                    totalScore,
                    gamesPlayed
            ));
        }
        result.sort(Comparator.comparingInt(LeaderboardEntry::averageScore).reversed());

        return result;
    }
}
