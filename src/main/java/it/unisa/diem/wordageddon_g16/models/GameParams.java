package it.unisa.diem.wordageddon_g16.models;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Classe interna che incapsula i parametri di una partita.
 * <p>
 * Rappresenta i parametri generati automaticamente per una partita in corso in base alla difficoltà scelta.
 * Contiene difficoltà, timer, documenti selezionati e numero di domande.
 * </p>
 */
public class GameParams implements Serializable {
    private static final Random random = new Random();
    private final Duration timer;
    private final List<Document> documents;
    private final int questionCount;
    private final Difficulty difficulty;

    public Duration getTimer() {
        return timer;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Classe di supporto per la gestione della difficoltà.
     * <p>
     * Calcola in modo progressivo l'influenza della difficoltà su vari parametri
     * </p>
     */
    public static class DifficultyIndex {
        private  float value;

        /**
         * Costruisce un nuovo DifficultyIndex.
         */
        public DifficultyIndex() {
            this.value = 1;
        }

        /**
         * Restituisce un valore casuale in base alla difficoltà corrente,
         * e lo sottrae dal valore disponibile.
         *
         * @return valore parziale generato casualmente
         */
        public float getNext() {
            var result = random.nextFloat(value);
            value -= result;
            return result;
        }

        /**
         * Fornisce la quantità di difficoltà ancora disponibile.
         *
         * @return valore rimanente
         */
        public float getRemaining() {
            return value;
        }
    }

    /**
     * Costruisce i parametri di gioco con valori specifici. Viene utilizzato per il restore di una partita interrotta.
     *
     * @param difficulty    difficoltà della partita
     * @param documents     lista di documenti selezionati per la partita
     * @param timer         durata del timer per la partita
     * @param questionCount numero di domande da generare per la partita
     */
    public GameParams(Difficulty difficulty, List<Document> documents, Duration timer, int questionCount) {
        this.difficulty = difficulty;
        var di = new DifficultyIndex();
        this.documents = Collections.unmodifiableList(new ArrayList<>(documents));
        this.timer = timer;
        this.questionCount = questionCount;
    }
}
