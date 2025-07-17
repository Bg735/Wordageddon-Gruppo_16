package it.unisa.diem.wordageddon_g16.models;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta lo stato temporaneo di una sessione di gioco interrotta in Wordageddon.
 * <p>
 * Questa record viene serializzato per permettere il salvataggio e il recupero preciso
 * della partita nel punto in cui l'utente ha interrotto.
 * </p>
 *
 * @param user                L'utente a cui appartiene la sessione
 * @param difficulty          Livello di difficoltà della partita
 * @param documents           Lista di documenti selezionati per la sessione
 * @param questions           Lista delle domande generate
 * @param domandaRisposte     Mappa domanda --> risposta data (-1 se saltata)
 * @param currentQuestionIndex Indice della domanda attuale
 * @param questionStartTime   Istante d'inizio delle domande (può essere null se non usato)
 * @param score               Punteggio ottenuto alla fine della sessione
 */
public record GameSessionState(
        User user,
        Difficulty difficulty,
        List<Document> documents,
        List<Question> questions,
        Map<Question, Integer> domandaRisposte,
        int currentQuestionIndex,
        LocalDateTime questionStartTime,
        int score,
        int questionCount

) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public User user() {
        return user;
    }

    @Override
    public Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public List<Document> documents() {
        return documents;
    }

    @Override
    public List<Question> questions() {
        return questions;
    }

    @Override
    public Map<Question, Integer> domandaRisposte() {
        return domandaRisposte;
    }

    @Override
    public int currentQuestionIndex() {
        return currentQuestionIndex;
    }

    @Override
    public LocalDateTime questionStartTime() {
        return questionStartTime;
    }

    @Override
    public int score() {
        return score;
    }

    @Override
    public int questionCount() {
        return questionCount;
    }
}
