package it.unisa.diem.wordageddon_g16.models;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Rappresenta un documento testuale con titolo, percorso e numero di parole.
 * <p>
 * Due documenti sono considerati uguali se hanno lo stesso percorso.
 *
 * @param title     il titolo del documento
 * @param path      il percorso del file del documento
 * @param wordCount il numero di parole nel documento
 */
public record Document(String title, Path path, int wordCount) {

    /**
     * Verifica l'uguaglianza tra questo documento e un altro oggetto.
     * Due documenti sono uguali se hanno lo stesso percorso.
     *
     * @param o l'oggetto da confrontare
     * @return {@code true} se i percorsi coincidono, {@code false} altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Document document)) return false;
        return Objects.equals(path, document.path);
    }

    /**
     * Restituisce l'hash code del documento, calcolato sul percorso.
     *
     * @return l'hash code basato sul percorso
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

    /**
     * Restituisce il titolo del documento.
     *
     * @return il titolo
     */
    @Override
    public String title() {
        return title;
    }

    /**
     * Restituisce il percorso del documento.
     *
     * @return il percorso come {@link Path}
     */
    @Override
    public Path path() {
        return path;
    }

    /**
     * Restituisce il numero di parole del documento.
     *
     * @return il conteggio delle parole
     */
    @Override
    public int wordCount() {
        return wordCount;
    }
}
