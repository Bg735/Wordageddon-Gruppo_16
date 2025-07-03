

/*
package it.unisa.diem.wordageddon_g16.services;

import it.unisa.diem.wordageddon_g16.db.GameReportDAO;
import it.unisa.diem.wordageddon_g16.db.UserDAO;
import it.unisa.diem.wordageddon_g16.models.AppContext;
import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LeaderboardService {

    public record LeaderboardEntry(
        String username,
        Difficulty favouriteDifficulty, // nullable
        int averageScore,
        int totalScore,
        int gamesPlayed
    ){}

    private final GameReportDAO gameReportDAO;
    private final List<User> users;
    private final User currentUser;

    public LeaderboardService(AppContext context, GameReportDAO gameReportDAO, UserDAO userDAO) {
        this.currentUser = context.getCurrentUser();
        this.gameReportDAO = gameReportDAO;
        this.users = userDAO.selectAll();
    }

    public List<LeaderboardEntry> getGloablLeaderboard(){
        return getLeaderboardBase(null);
    }

    public List<LeaderboardEntry> getLeaderboardByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty cannot be null");
        }
        return getLeaderboardBase(difficulty);
    }

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
                totalScore = reports.stream().mapToInt(GameReport::getScore).sum();
                gamesPlayed = reports.size();
                averageScore = totalScore / gamesPlayed;
            }
            Difficulty favouriteDifficulty = null;
            if(difficulty!=null)
                favouriteDifficulty = reports.stream()
                    .map(GameReport::getDifficulty)
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
        result.sort(Comparator.comparingInt(LeaderboardEntry::totalScore));
        return result;
    }
}
*/