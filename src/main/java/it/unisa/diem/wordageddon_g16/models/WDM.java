package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class WDM {
    private final Document document;
    private final Map<String,Integer> words;

    public WDM(Document document, Map<String, Integer> words) {
        this.document = document;
        this.words = words;
    }

    public WDM(Document document, Collection<String> stopWords) {
        this.document = document;
        words = calculateWordMatrix(document,stopWords);
    }

    private Map<String,Integer> calculateWordMatrix(Document document, Collection<String> stopWords) {
        Map<String, Integer> wordMap = new HashMap<>();
        try (Scanner scanner = new Scanner(new InputStreamReader(Objects.requireNonNull(WDM.class.getClassLoader().getResourceAsStream(document.filename()))))) {
            scanner.useDelimiter("\\s+");
            while (scanner.hasNext()) {
                String word = scanner.next().replaceAll("\\p{Punct}", "");
                if (!word.isEmpty() && !stopWords.contains(word.toLowerCase())) {
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
