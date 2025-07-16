package it.unisa.diem.wordageddon_g16.models;

import java.util.Objects;
import java.io.Serializable;


/**
 * Classe che rappresenta un utente dell'applicazione Wordageddon.
 * <p>
 * Include nome utente, password e flag di amministratore.
 * <br>
 * La classe implementa {@link Serializable} per supportare il salvataggio e caricamento della sessione.
 */
public class User implements Serializable {
    /**
     * Nome utente dell'utente.
     */
    private String name;

    /**
     * Password associata all'utente.
     */
    private String password;
    /**
     * Indica se l'utente ha privilegi da amministratore.
     */
    private boolean isAdmin;

    /**
     * Costruisce un utente con nome, password e ruolo amministrativo.
     *
     * @param name      nome utente
     * @param password  password associata
     * @param isAdmin   {@code true} se l'utente ha privilegi da amministratore
     */
    public User(String name, String password, boolean isAdmin) {
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
    }
    /**
     * Restituisce il nome utente.
     *
     * @return nome utente
     */
    public String getName() {
        return name;
    }

    /**
     * Restituisce la password dell'utente.
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }
    /**
     * Verifica se l'utente è un amministratore.
     *
     * @return {@code true} se l'utente è admin, {@code false} altrimenti
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Verifica l'uguaglianza tra utenti basata sul nome utente.
     *
     * @param o oggetto da confrontare
     * @return {@code true} se i nomi coincidono, {@code false} altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name);
    }
    /**
     * Calcola l'hash code dell'utente basato sul nome.
     *
     * @return valore hash
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    /**
     * Aggiorna il nome dell'utente.
     *
     * @param name nuovo nome
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Aggiorna la password dell'utente.
     *
     * @param password nuova password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Imposta lo stato amministrativo dell'utente.
     *
     * @param admin {@code true} se deve diventare admin
     */
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}