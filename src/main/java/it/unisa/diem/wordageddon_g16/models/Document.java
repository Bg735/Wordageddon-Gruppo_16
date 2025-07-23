package it.unisa.diem.wordageddon_g16.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Rappresenta un documento testuale nell'applicazione Wordageddon.
 * <p>
 * Ogni documento è identificato dal suo percorso ({@code filename}), ha un titolo e un conteggio di parole.
 * Due documenti sono considerati uguali se condividono lo stesso percorso.
 *
 * @param filename  nome del file associato al documento
 * @param title     titolo descrittivo del documento
 * @param wordCount numero di parole contenute nel documento
 */
public record Document(String filename, String title, Integer wordCount) implements Serializable {

    /**
     * Verifica se questo documento è uguale a un altro oggetto in base al percorso ({@code filename}).
     *
     * @param o oggetto da confrontare
     * @return {@code true} se i percorsi coincidono, {@code false} altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Document document)) return false;
        return Objects.equals(filename, document.filename);
    }

    /**
     * Restituisce l'hash code del documento calcolato sul campo {@code filename}.
     *
     * @return valore hash del percorso
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(filename);
    }

    /**
     * Restituisce il titolo del documento.
     *
     * @return titolo come {@code String}
     */
    @Override
    public String title() {
        return title;
    }

     /**
     * Restituisce il nome del file associato al documento.
     *
     * @return percorso del file come {@code String}
     */
    @Override
    public String filename() {
        return filename;
    }

    /**
     * Restituisce il numero di parole contenute nel documento.
     *
     * @return conteggio parole come {@code Integer}
     */
    @Override
    public Integer wordCount() {
        return wordCount;
    }
}
