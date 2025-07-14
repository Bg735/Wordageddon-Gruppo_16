package it.unisa.diem.wordageddon_g16.db.contracts;

import java.util.Set;

/**
 * Interfaccia per la gestione delle stopword salvate nel database.
 * <p>
 * Le stopword sono parole da ignorare nell'elaborazione del testo.
 */
public interface StopWordDAO extends DAO<String> {

    /**
     * Recupera tutte le stopword presenti nel database.
     *
     * @return un insieme di tutte le parole da escludere dall'analisi
     */
    @Override
    Set<String> selectAll();

    /**
     * Verifica se la tabella delle stopword è vuota.
     *
     * @return {@code true} se non ci sono stopword salvate, {@code false} altrimenti
     */
    boolean isEmpty();
}
