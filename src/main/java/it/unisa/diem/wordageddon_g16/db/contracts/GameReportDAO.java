package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * Interfaccia per la gestione dei report di gioco.
 * <p>
 * Estende {@link DAO} e fornisce metodi specifici per recuperare un {@link GameReport}
 * tramite l'utente e il timestamp.
 */
public interface GameReportDAO extends DAO<GameReport> {

    /**
     * Recupera un report di gioco tramite utente e timestamp.
     *
     * @param user l'utente che ha effettuato la partita
     * @param timestamp la data e ora in cui il report è stato registrato
     * @return un {@code Optional} contenente il {@link GameReport} se esistente, altrimenti vuoto
     */
    Optional<GameReport> selectBy(User user, Timestamp timestamp);
}
