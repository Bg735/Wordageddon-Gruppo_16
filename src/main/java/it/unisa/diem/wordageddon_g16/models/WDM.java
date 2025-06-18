package it.unisa.diem.wordageddon_g16.models;

import java.util.HashMap;
import java.util.Map;

public class WDM {
    private final Document document;
    private final Map<String,Integer> words;

    public WDM(Document document) {
        this.document = document;
        words = new HashMap<>();
    }

    public WDM(Document document, Map<String, Integer> words) {
        this.document = document;
        this.words = words;
    }

    public Map<String, Integer> getWords() {
        return words;
    }

    public Document getDocument() {
        return document;
    }
}
