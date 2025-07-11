package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.utility.Config;
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
    private final Map<String,Integer> words;

    /**
     * Costruisce un oggetto WDM associando direttamente una mappa di frequenze a un documento.
     *
     * @param document il documento di riferimento
     * @param words la mappa delle parole e delle loro frequenze
     */
    public WDM(Document document, Map<String, Integer> words) {
        this.document = document;
        this.words = words;
    }

    /**
     * Costruisce un oggetto WDM analizzando il documento specificato ed escludendo le stopwords fornite.
     * La mappa delle frequenze viene calcolata automaticamente.
     *
     * @param document il documento da analizzare
     * @param stopWords la collezione di parole da escludere dall'analisi (stopwords)
     */
    public WDM(Document document, Collection<String> stopWords) {
        this.document = document;
        words = calculateWordMatrix(document, stopWords);
    }

    /**
     * Analizza il contenuto del documento, calcolando la frequenza delle parole significative
     * (escludendo le stopwords) e restituendo una mappa parola→frequenza.
     *
     * @param document il documento da analizzare
     * @param stopWords la collezione di parole da escludere dall'analisi
     * @return una mappa contenente le parole significative e le rispettive frequenze
     */
    private Map<String,Integer> calculateWordMatrix(Document document, Collection<String> stopWords) {
        Map<String, Integer> wordMap = new HashMap<>();

        // I documenti sono salvati nella cartella "uploads/documents"
        Path path = Path.of(Config.get(Config.Props.DOCUMENTS_DIR) + document.filename());

        try (Scanner scanner = new Scanner(Files.newBufferedReader(path))) {
            scanner.useDelimiter("\\s+");
            while (scanner.hasNext()) {
                String word = scanner.next().replaceAll("\\p{Punct}", "");
                if (!word.isEmpty() && !stopWords.contains(word.toLowerCase())) {
                    wordMap.put(word, wordMap.getOrDefault(word, 0) + 1);
                }
            }
        } catch(IOException e) {
            SystemLogger.log("Error reading document " + path, e);
        }
        return wordMap;
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
