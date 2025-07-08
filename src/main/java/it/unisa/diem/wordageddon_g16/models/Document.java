package it.unisa.diem.wordageddon_g16.models;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Rappresenta un documento testuale caricato nel sistema Wordageddon.
 * <p>
 * Ogni documento è identificato da un id univoco e contiene informazioni
 * quali titolo, nome file sorgente e numero di parole.
 * L'uguaglianza tra documenti è definita esclusivamente sull'id.
 *
 * @param id        identificativo univoco del documento
 * @param title     titolo del documento
 * @param path  nome del file sorgente associato al documento
 * @param wordCount numero di parole presenti nel documento
 */
public record Document(long id, String title, String path, int wordCount) {

    // Costruttore non canonico: imposta 0 l'id
    public Document(String title, String path, int wordCount){
        this(0, title, path, wordCount);
    }

    /**
     * Determina se questo documento è uguale a un altro oggetto.
     * Due documenti sono considerati uguali se hanno lo stesso id.
     *
     * @param o l'oggetto da confrontare
     * @return true se l'oggetto è un Document con lo stesso id, false altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id == document.id;
    }

    /**
     * Restituisce il codice hash di questo documento, basato sull'id.
     *
     * @return il codice hash calcolato sull'id
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public int wordCount() {
        return wordCount;
    }
}