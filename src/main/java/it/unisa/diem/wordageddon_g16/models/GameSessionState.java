package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.services.GameService;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta lo stato completo di una sessione di gioco interrotta in Wordageddon.
 * <p>
 * Questa classe viene serializzata per permettere il salvataggio e il recupero preciso
 * della partita nel punto in cui l'utente ha interrotto.
 * </p>
 */
public class GameSessionState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * L'utente a cui appartiene la sessione di gioco.
     */
    private User user;

    /**
     * Difficoltà impostata per la partita.
     */
    private Difficulty difficulty;

    /**
     * Elenco dei documenti selezionati per la sessione.
     */
    private List<Document> documents;

    /**
     * Lista delle domande generate e visualizzate nella sessione corrente.
     */
    private List<GameService.Question> questions;

    /**
     * Mappa tra ogni domanda e l'indice della risposta data dall'utente.
     * Il valore è -1 se la domanda è stata saltata.
     */
    private Map<GameService.Question, Integer> domandaRisposte;

    /**
     * Indice della domanda attualmente in corso (0-based).
     */
    private int currentQuestionIndex;

    /**
     * Momento di avvio della fase domande, o tempo usato finora per la partita.
     */
    private LocalDateTime questionStartTime;

    /**
     * Costruttore vuoto richiesto per la deserializzazione.
     */

    private int score;
    public GameSessionState() {}

    /**
     * Costruttore completo.
     *
     * @param user                L'utente a cui appartiene la sessione
     * @param difficulty          Livello di difficoltà della partita
     * @param documents           Lista di documenti selezionati per la sessione
     * @param questions           Lista delle domande generate
     * @param domandaRisposte     Mappa domanda --> risposta data (-1 se saltata)
     * @param currentQuestionIndex Indice della domanda attuale
     * @param questionStartTime   Istante d'inizio delle domande (può essere null se non usato)
     */
    public GameSessionState(User user,
                            Difficulty difficulty,
                            List<Document> documents,
                            List<GameService.Question> questions,
                            Map<GameService.Question, Integer> domandaRisposte,
                            int currentQuestionIndex,
                            LocalDateTime questionStartTime,
                            int score
    ) {
        this.user = user;
        this.difficulty = difficulty;
        this.documents = documents;
        this.questions = questions;
        this.domandaRisposte = domandaRisposte;
        this.currentQuestionIndex = currentQuestionIndex;
        this.questionStartTime = questionStartTime;
        this.score = score;
    }


    /**
     * Restituisce il punteggio ottenuto alla fine della sessione.
     * @return punteggio della sessione
     */
    public int getScore() {
        return score;
    }

    /**
     * Imposta il punteggio della sessione.
     * @param score nuovo punteggio da impostare
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Restituisce l'istante di avvio della sessione domande o il tempo usato.
     * @return data e ora di inizio domande
     */
    public LocalDateTime getQuestionStartTime() {
        return questionStartTime;
    }

    /**
     * Imposta il tempo usato nella sessione o l'istante di avvio.
     * @param questionStartTime nuovo valore per il tempo usato
     */
    public void setQuestionStartTime(LocalDateTime questionStartTime) {
        this.questionStartTime = questionStartTime;
    }

    /**
     * Restituisce l’utente associato alla sessione.
     * @return utente della sessione
     */
    public User getUser() {
        return user;
    }

    /**
     * Imposta l’utente associato alla sessione.
     * @param user nuovo utente
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Restituisce la difficoltà impostata.
     * @return livello di difficoltà
     */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Imposta la difficoltà della sessione.
     * @param difficulty nuova difficoltà
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * Restituisce la lista dei documenti della partita.
     * @return lista documenti
     */
    public List<Document> getDocuments() {
        return documents;
    }

    /**
     * Imposta la lista dei documenti della sessione.
     * @param documents nuova lista di documenti
     */
    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    /**
     * Restituisce la lista delle domande della partita.
     * @return lista domande
     */
    public List<GameService.Question> getQuestions() {
        return questions;
    }

    /**
     * Imposta la lista delle domande della sessione.
     * @param questions nuova lista di domande
     */
    public void setQuestions(List<GameService.Question> questions) {
        this.questions = questions;
    }

    /**
     * Restituisce la mappa domanda-risposta (indice della risposta data dall'utente oppure -1 se saltata).
     * @return mappa domanda --> risposta data
     */
    public Map<GameService.Question, Integer> getDomandaRisposte() {
        return domandaRisposte;
    }

    /**
     * Imposta la mappa domanda-risposta per la sessione.
     * @param domandaRisposte nuova mappa domanda-risposta
     */
    public void setDomandaRisposte(Map<GameService.Question, Integer> domandaRisposte) {
        this.domandaRisposte = domandaRisposte;
    }

    /**
     * Restituisce l'indice della domanda attuale (0-based).
     * @return indice domanda corrente
     */
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    /**
     * Imposta l'indice della domanda corrente.
     * @param currentQuestionIndex nuovo indice domanda
     */
    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }
}
