package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class WDM {
    private final Document document;
    private final Map<String,Integer> words;

    public WDM(Document document) {
        this.document = document;
        words = calculateWordMatrix(document);
    }

    public WDM(Document document, Map<String, Integer> words) {
        this.document = document;
        this.words = words;
    }

    private Map<String,Integer> calculateWordMatrix(Document document) {
        Map<String, Integer> wordMap = new HashMap<>();
        try (Scanner scanner = new Scanner(new InputStreamReader(Objects.requireNonNull(WDM.class.getClassLoader().getResourceAsStream(document.filename()))))) {
            while (scanner.hasNext()) {
                String word = scanner.next().replaceAll("\\p{Punct}", "");
                if (!word.isEmpty()) {
                    wordMap.put(word, wordMap.getOrDefault(word, 0) + 1);
                }
            }
        }
        return wordMap;
    }




    public Map<String, Integer> getWords() {
        return words;
    }

    public Document getDocument() {
        return document;
    }
}
