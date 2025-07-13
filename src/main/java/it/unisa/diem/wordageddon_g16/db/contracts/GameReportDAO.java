package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.User;

import java.sql.Timestamp;
import java.util.Optional;

public interface GameReportDAO extends DAO<GameReport> {
    Optional<GameReport> selectBy(User user, Timestamp timestamp);
}
