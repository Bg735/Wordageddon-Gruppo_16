package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.Document;

import java.util.Optional;

/**
 * Interfaccia specifica per l'accesso ai dati dei documenti nel database.
 * <p>
 * Estende l'interfaccia {@link DAO} con operazioni dedicate ai documenti,
 * identificati univocamente tramite il loro nome file.
 */
public interface DocumentDAO extends DAO<Document> {

    /**
     * Recupera un documento dal database in base al suo nome file.
     *
     * @param filename il nome del file associato al documento da cercare
     * @return un {@code Optional} contenente il documento trovato, oppure vuoto se non esiste
     */
    Optional<Document> selectBy(String filename);

    /**
     * Verifica se la tabella dei documenti è vuota.
     *
     * @return {@code true} se non ci sono documenti salvati, {@code false} altrimenti
     */
    boolean isEmpty();
}
