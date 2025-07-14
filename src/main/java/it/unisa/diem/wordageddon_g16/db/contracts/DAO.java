package it.unisa.diem.wordageddon_g16.db.contracts;

import java.util.Collection;
import java.util.Optional;

/**
 * Interfaccia generica per il pattern Data Access Object (DAO).
 * <p>
 * Fornisce un insieme standard di operazioni CRUD (Create, Read, Update, Delete)
 * per la gestione di oggetti persistenti di tipo {@code T}, indipendentemente dalla loro origine o struttura.
 * Le classi che implementano questa interfaccia interagiscono direttamente con una sorgente dati
 * (es. database relazionale) per fornire un'astrazione pulita sull'accesso ai dati.
 *
 * @param <T> il tipo di oggetto gestito dal DAO
 */
public interface DAO<T> {

    /**
     * Recupera tutti gli oggetti gestiti dal DAO dalla sorgente dati.
     *
     * @return una collezione contenente tutte le istanze di tipo {@code T} presenti nel database
     */
    Collection<T> selectAll();

    /**
     * Inserisce un nuovo oggetto nel database.
     * <p>
     * L'oggetto {@code t} viene salvato come nuova riga nella tabella corrispondente,
     * secondo le regole e i vincoli imposti dallo schema del database.
     *
     * @param t l'oggetto da inserire nel database
     */
    void insert(T t);

    /**
     * Aggiorna i dati relativi a un oggetto esistente nel database.
     * <p>
     * L'identificazione dell'oggetto da aggiornare dipende dalla chiave primaria o da un identificatore univoco.
     *
     * @param t l'oggetto contenente i nuovi dati da salvare
     */
    void update(T t);

    /**
     * Elimina un oggetto dal database.
     * <p>
     * L'oggetto specificato viene rimosso dalla tabella in base al suo identificativo univoco.
     *
     * @param t l'oggetto da eliminare dalla sorgente dati
     */
    void delete(T t);
}
