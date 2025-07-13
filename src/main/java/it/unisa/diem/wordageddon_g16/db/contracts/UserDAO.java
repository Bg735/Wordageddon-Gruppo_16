package it.unisa.diem.wordageddon_g16.db.contracts;

import it.unisa.diem.wordageddon_g16.models.User;

import java.util.Optional;

public interface UserDAO extends DAO<User> {
    Optional<User> selectBy(String username);
}
