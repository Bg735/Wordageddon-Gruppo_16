package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.WDM;

import java.util.Optional;

/**
 * Interfaccia per la gestione delle WDM (Word Document Matrix),
 * che associano a ciascun documento le parole rilevanti e la loro frequenza.
 */
public interface WdmDAO extends DAO<WDM> {

    /**
     * Recupera una matrice parola-documento (WDM) a partire da un documento.
     *
     * @param document il documento di cui si vogliono ottenere le frequenze delle parole
     * @return un {@code Optional} contenente la WDM associata, o vuoto se non esiste
     */
    Optional<WDM> selectBy(Document document);
}
