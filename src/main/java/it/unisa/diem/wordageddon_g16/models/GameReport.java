package it.unisa.diem.wordageddon_g16.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta un report di una sessione di gioco nell'applicazione Wordageddon.
 * <p>
 * Include informazioni sul giocatore, i documenti utilizzati, la difficoltà selezionata,
 * il tempo impiegato, il numero di domande e il punteggio ottenuto.
 * Viene utilizzato per memorizzare i risultati delle partite completate.
 * </p>
 *
 * @param user          utente che ha effettuato la sessione
 * @param documents     lista dei documenti selezionati per la partita
 * @param timestamp     data e ora di fine sessione di gioco
 * @param difficulty    livello di difficoltà della sessione
 * @param maxTime       tempo massimo previsto per rispondere alle domande
 * @param usedTime      tempo effettivamente impiegato nel rispondere alle domande
 * @param questionCount numero totale di domande generate
 * @param score         punteggio ottenuto alla fine della sessione
 */
public record GameReport (
    User user,
    List<Document> documents,
    LocalDateTime timestamp,
    Difficulty difficulty,
    Duration maxTime,
    Duration usedTime,
    int questionCount,
    int score
){
    /**
     * Costruttore compatto per il record {@code GameReport}.
     * <p>
     * Se la lista dei documenti è {@code null}, viene inizializzata come lista vuota
     * per evitare {@code NullPointerException} in fasi successive.
     */
    public GameReport {
        if (documents == null) {
            documents = new ArrayList<>();
        }
    }

    /**
     * Restituisce l’utente che ha giocato la partita.
     *
     * @return istanza di {@link User} associata alla sessione
     */
    @Override
    public User user() {
        return user;
    }

    /**
     * Restituisce la lista dei documenti utilizzati nella sessione.
     *
     * @return lista di {@link Document}
     */
    @Override
    public List<Document> documents() {
        return documents;
    }

    /**
     * Restituisce la data e ora di avvio della sessione di gioco.
     *
     * @return istanza di {@link LocalDateTime}
     */
    @Override
    public LocalDateTime timestamp() {
        return timestamp;
    }

    /**
     * Restituisce il livello di difficoltà selezionato per la partita.
     *
     * @return valore di {@link Difficulty}
     */
    @Override
    public Difficulty difficulty() {
        return difficulty;
    }

    /**
     * Restituisce il tempo massimo concesso per la sessione di gioco, ossia il tempo per rispondere alle domande.
     *
     * @return tempo massimo come {@link Duration}
     */
    @Override
    public Duration maxTime() {
        return maxTime;
    }

    /**
     * Restituisce il tempo effettivamente impiegato dal giocatore nel rispondere alle domande.
     *
     * @return tempo utilizzato come {@link Duration}
     */
    @Override
    public Duration usedTime() {
        return usedTime;
    }

    /**
     * Restituisce il numero totale di domande generate nella sessione.
     *
     * @return numero di domande
     */
    @Override
    public int questionCount() {
        return questionCount;
    }

    /**
     * Restituisce il punteggio ottenuto dal giocatore durante la sessione.
     *
     * @return punteggio finale
     */
    @Override
    public int score() {
        return score;
    }
}