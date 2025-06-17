package it.unisa.diem.wordageddon_g16.models.interfaces;

import it.unisa.diem.wordageddon_g16.db.DAO;

public interface Repository {
    <T> DAO<T> getDAO(String category);
    void close();
}
