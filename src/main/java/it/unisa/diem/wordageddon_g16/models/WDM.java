package it.unisa.diem.wordageddon_g16.models;

import java.util.HashMap;
import java.util.Map;

public class WDM {
    private final Document document;
    private final Map<String,Integer> words;

    public WDM(Document document) {
        this.document = document;
        words = calculateWordMatrix(document);
    }

    private Map<String,Integer> calculateWordMatrix(Document document) {
        Map<String, Integer> wordMap = new HashMap<>();




        for (String word : document.getWords()) {
            wordMap.put(word, wordMap.getOrDefault(word, 0) + 1);
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
