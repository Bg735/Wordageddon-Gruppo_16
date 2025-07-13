package it.unisa.diem.wordageddon_g16.db.contracts;

import java.util.Collection;
import java.util.Optional;

/**
 * Interfaccia generica per il pattern Data Access Object (DAO).
 * Definisce le operazioni CRUD di base per la gestione degli oggetti di tipo T nel database.
 *
 * @param <T> il tipo di oggetto gestito dal DAO
 */
public interface DAO<T> {

    /**
     * Recupera tutti gli oggetti gestiti dal DAO.
     *
     * @return una collezione contenente tutti gli oggetti trovati
     */
    Collection<T> selectAll();

    /**
     * Inserisce un nuovo oggetto nel database.
     *
     * @param t l'oggetto da inserire
     */
    void insert(T t);

    /**
     * Aggiorna le informazioni di un oggetto esistente nel database.
     *
     * @param t l'oggetto da aggiornare
     */
    void update(T t);

    /**
     * Elimina un oggetto dal database.
     *
     * @param t l'oggetto da eliminare
     */
    void delete(T t);
}