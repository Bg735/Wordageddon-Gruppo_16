package it.unisa.diem.wordageddon_g16.models;

import java.util.List;

/**
 * Rappresenta una domanda del quiz, con testo, risposte e indice della risposta corretta.
 */
public record Question(
        String text,
        List<String> answers,
        int correctAnswerIndex
) {
    /**
     * Tipologie di domande disponibili nel quiz.
     */
    public enum QuestionType {
        ABSOLUTE_FREQUENCY(1f), // Quante volte appare una parola
        WHICH_MORE(0.5f),       // Quale parola appare più spesso tra quelle proposte
        WHICH_LESS(0.5f),       // Quale parola appare meno spesso tra quelle proposte
        WHICH_DOCUMENT(1f),     // Quale documento contiene una parola
        WHICH_ABSENT(1f);       // Quale parola non è presente in nessun documento

        private final float weight;

        QuestionType(float weight) {
            this.weight = weight;
        }

        /**
         * Restituisce una tipologia di domanda casuale.
         *
         * @return tipo di domanda scelto casualmente
         */
        public static QuestionType getRandomType() {
            var types = values();
            return types[(int) (Math.random() * types.length)];
        }
    }

    /**
     * Crea una nuova domanda.
     *
     * @param text               testo della domanda
     * @param answers            elenco delle possibili risposte
     * @param correctAnswerIndex indice della risposta corretta
     * @return una nuova istanza di Question
     * @throws IllegalArgumentException se i parametri non sono validi
     */
    public static Question create(String text, List<String> answers, int correctAnswerIndex) {
        if (text == null || answers == null || correctAnswerIndex < 0 || correctAnswerIndex >= answers.size()) {
            throw new IllegalArgumentException("Invalid question parameters");
        }
        return new Question(text, answers, correctAnswerIndex);
    }
}
