package it.unisa.diem.wordageddon_g16.db.contracts;

/**
 * Interfaccia che definisce un repository centralizzato per l'accesso ai DAO.
 * <p>
 * Ogni implementazione di questa interfaccia fornisce un modo per ottenere DAO
 * specifici in base alla categoria richiesta.
 */
public interface Repository {

    /**
     * Restituisce un DAO per una specifica categoria di oggetti.
     *
     * @param <T>    il tipo di oggetto gestito dal DAO
     * @param <TDAO> il tipo del DAO da restituire
     * @param category la categoria del DAO (usata come chiave per il recupero)
     * @return il DAO corrispondente alla categoria specificata
     */
    <T, TDAO extends DAO<T>> TDAO getDAO(String category);

    /**
     * Chiude tutte le risorse associate al repository, come le connessioni al database.
     */
    void close();
}
