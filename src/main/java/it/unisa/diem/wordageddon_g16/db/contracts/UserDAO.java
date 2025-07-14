package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.User;

import java.util.Optional;

/**
 * Interfaccia per la gestione degli utenti nel database.
 * <p>
 * Fornisce metodi specifici per recuperare un utente tramite username.
 */
public interface UserDAO extends DAO<User> {

    /**
     * Recupera un utente in base al nome.
     *
     * @param username il nome utente da cercare
     * @return un {@code Optional} contenente l'utente se trovato, oppure vuoto
     */
    Optional<User> selectBy(String username);
}
