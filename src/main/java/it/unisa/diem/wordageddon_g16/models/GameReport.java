package it.unisa.diem.wordageddon_g16.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
}