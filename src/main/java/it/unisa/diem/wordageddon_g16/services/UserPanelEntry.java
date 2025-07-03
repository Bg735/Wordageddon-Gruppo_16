package it.unisa.diem.wordageddon_g16.services;

public class UserPanelEntry {
    private final String difficulty;
    private final int score;
    private final String time;

    public String getDifficultyForCurrentUser() {
        return difficulty;
    }
    public int getScoreForCurrentUser() {
        return score;
    }
    public String getTimeForCurrentUser() {
        return time;
    }

    public UserPanelEntry(String difficulty, int score, String time) {
        this.difficulty = difficulty;
        this.score = score;
        this.time = time;
    }

}
