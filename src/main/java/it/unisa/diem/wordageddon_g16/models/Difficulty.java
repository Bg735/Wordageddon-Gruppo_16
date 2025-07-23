package it.unisa.diem.wordageddon_g16.models;

/**
 * Enum che rappresenta i livelli di difficoltà disponibili nel gioco Wordageddon.
 * Ogni valore è associato a una stringa descrittiva e a un punteggio massimo
 * ottenibile tramite il metodo {@link #getMaxScoreDifficulty(Difficulty)}.
 *
 * Livelli disponibili:
 * <ul>
 *   <li>{@code EASY} – Facile</li>
 *   <li>{@code MEDIUM} – Medio</li>
 *   <li>{@code HARD} – Difficile</li>
 * </ul>
 */
public enum  Difficulty {
    EASY("Facile"),
    MEDIUM("Medio"),
    HARD("Difficile");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    /**
     * Restituisce il punteggio massimo ottenibile per un dato livello di difficoltà.
     *
     * @param d livello di difficoltà
     * @return punteggio massimo associato ({@code 100}, {@code 200} o {@code 300})
     */
    public static int getMaxScoreDifficulty(Difficulty d) {
        return switch (d) {
            case EASY -> 100;
            case MEDIUM -> 200;
            case HARD -> 300;
        };
    }

    /**
     * Restituisce l’etichetta testuale associata alla difficoltà.
     *
     * @return stringa leggibile come {@code "Facile"}, {@code "Medio"} o {@code "Difficile"}
     */
    @Override
    public String toString() {
        return label;
    }
}
