package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.services.GameService;

import java.io.Serializable;
import java.util.List;

/**
 * Rappresenta una domanda a risposta multipla generata dal {@link GameService}.
 *
 * Ogni {@code Question} contiene:
 * <ul>
 *   <li>Il testo della domanda ({@code text})</li>
 *   <li>Un elenco di possibili risposte ({@code answers})</li>
 *   <li>L'indice della risposta corretta ({@code correctAnswerIndex})</li>
 * </ul>
 * Utilizzato nella fase quiz per testare la comprensione dell'utente sui documenti letti.
 */
public record Question(
        String text,
        List<String> answers,
        int correctAnswerIndex
) implements Serializable {
    /**
     * Enum interno che definisce le diverse tipologie di domande generabili.
     * Ogni tipo ha un {@code weight} che ne indica la rilevanza nella fase di generazione.
     * Tipologie disponibili:
     * <ul>
     *   <li>{@code ABSOLUTE_FREQUENCY} – Quante volte appare una parola</li>
     *   <li>{@code WHICH_MORE} – Quale parola appare più spesso</li>
     *   <li>{@code WHICH_LESS} – Quale parola appare meno spesso</li>
     *   <li>{@code WHICH_DOCUMENT} – In quale documento compare una parola</li>
     *   <li>{@code WHICH_ABSENT} – Quale parola è assente da tutti i documenti</li>
     * </ul>
     */
    public enum QuestionType {
        ABSOLUTE_FREQUENCY(1f), // Quante volte appare una parola
        WHICH_MORE(0.5f),       // Quale parola appare più spesso tra quelle proposte
        WHICH_LESS(0.5f),       // Quale parola appare meno spesso tra quelle proposte
        WHICH_DOCUMENT(1f),     // Quale documento contiene una parola
        WHICH_ABSENT(1f);       // Quale parola non è presente in nessun documento

        private final float weight;

        /**
         * Costruisce una tipologia di domanda con peso associato.
         *
         * @param weight valore numerico che rappresenta il peso logico del tipo di domanda
         */
        QuestionType(float weight) {
            this.weight = weight;
        }
    }

    /**
     * Crea una nuova istanza di {@code Question} validando i parametri forniti.
     * La domanda è valida solo se:
     * <ul>
     *   <li>{@code text} non è {@code null}</li>
     *   <li>{@code answers} non è {@code null}</li>
     *   <li>{@code correctAnswerIndex} è compreso tra {@code 0} e {@code answers.size() - 1}</li>
     * </ul>
     * Se non rispettati, viene lanciata una {@link IllegalArgumentException}.
     *
     * @param text               testo della domanda
     * @param answers            lista delle possibili risposte
     * @param correctAnswerIndex indice della risposta corretta
     * @return istanza valida di {@code GameService.Question}
     * @throws IllegalArgumentException se i parametri sono invalidi
     */
    public static Question create(String text, List<String> answers, int correctAnswerIndex) {
        if (text == null || answers == null || correctAnswerIndex < 0 || correctAnswerIndex >= answers.size()) {
            throw new IllegalArgumentException("Invalid question parameters");
        }
        return new Question(text, answers, correctAnswerIndex);
    }
}
