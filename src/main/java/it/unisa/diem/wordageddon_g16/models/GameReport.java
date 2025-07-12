package it.unisa.diem.wordageddon_g16.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record GameReport (
    long id,
    User user,
    List<Document> documents,
    LocalDateTime timestamp,
    Difficulty difficulty,
    Duration maxTime,
    Duration usedTime,
    int questionCount,
    int score
){
    public GameReport {
        if (documents == null) {
            documents = new ArrayList<>();
        }
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public List<Document> documents() {
        return documents;
    }

    @Override
    public LocalDateTime timestamp() {
        return timestamp;
    }

    @Override
    public Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public Duration maxTime() {
        return maxTime;
    }

    @Override
    public Duration usedTime() {
        return usedTime;
    }

    @Override
    public int questionCount() {
        return questionCount;
    }

    @Override
    public int score() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GameReport that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}