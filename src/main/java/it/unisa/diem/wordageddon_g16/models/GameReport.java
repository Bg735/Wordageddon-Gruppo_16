package it.unisa.diem.wordageddon_g16.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameReport {
    private long id;
    private User user;
    private List<Document> documents;
    private LocalDateTime timestamp;
    private Difficulty difficulty;
    private Duration maxTime;
    private Duration usedTime;
    private int questionCount;
    private int score;

    public GameReport(long id, User user, LocalDateTime timestamp, Difficulty difficulty, Duration maxTime, Duration usedTime, int questionCount, int score) {
        this.id = id;
        this.user = user;
        this.timestamp = timestamp;
        this.difficulty = difficulty;
        this.maxTime = maxTime;
        this.usedTime = usedTime;
        this.questionCount = questionCount;
        this.score = score;
        this.documents = new ArrayList<>();
    }

    public GameReport(long id, User user, List<Document> documents, LocalDateTime timestamp, Difficulty difficulty, Duration maxTime, Duration usedTime, int questionCount, int score) {
        this.id = id;
        this.user = user;
        this.timestamp = timestamp;
        this.difficulty = difficulty;
        this.maxTime = maxTime;
        this.usedTime = usedTime;
        this.questionCount = questionCount;
        this.score = score;
        this.documents = documents;
    }

    public long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Duration getMaxTime() {
        return maxTime;
    }

    public Duration getUsedTime() {
        return usedTime;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getScore() {
        return score;
    }

    public Document[] getDocuments() {
        return (Document[]) documents.toArray();
    }
}