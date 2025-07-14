package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.utility.Resources;
import it.unisa.diem.wordageddon_g16.utility.SystemLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * La classe WDM (Word Document Matrix) rappresenta l'associazione tra un documento
 * e la mappa delle frequenze delle parole significative in esso contenute.
 * Permette di analizzare un documento escludendo le stopwords e di accedere
 * facilmente ai risultati dell'analisi.
 */
public class WDM {
    /**
     * Il documento analizzato.
     */
    private final Document document;

    /**
     * Mappa delle parole significative e delle rispettive frequenze nel documento.
     * La chiave è la parola, il valore è il numero di occorrenze.
     */
    private final Map<String, Integer> words;

    /**
     * Costruisce un oggetto WDM associando direttamente una mappa di frequenze a un documento.
     *
     * @param document il documento di riferimento
     * @param words    la mappa delle parole e delle loro frequenze
     */
    public WDM(Document document, Map<String, Integer> words) {
        this.document = document;
        this.words = words;
    }

    public WDM(Document doc, Set<String> stopWords) {
        String filename = doc.filename();
        String title = doc.title();
        words = new HashMap<>();
        int wordCount = 0;
        try (Scanner scanner = new Scanner(new StringReader(Resources.getDocumentContent(filename)))) {
            scanner.useDelimiter("\\s+");
            while (scanner.hasNext()) {
                String word = scanner.next().replaceAll("\\p{Punct}", "").toLowerCase();
                if (!word.isEmpty() && !stopWords.contains(word)) {
                    words.put(word, words.getOrDefault(word, 0) + 1);
                    wordCount++;
                }
            }
        } catch (IOException e) {
            SystemLogger.log("Errore durante l'analisi del documento " + filename, e);
            throw new RuntimeException(e);
        }
        this.document = new Document(filename, title, wordCount);
    }

    /**
     * Restituisce la mappa delle parole significative e delle loro frequenze.
     *
     * @return la mappa parola→frequenza
     */
    public Map<String, Integer> getWords() {
        return words;
    }

    /**
     * Restituisce il documento associato a questa analisi.
     *
     * @return il documento analizzato
     */
    public Document getDocument() {
        return document;
    }
}