package it.unisa.diem.wordageddon_g16.models.interfaces;

import it.unisa.diem.wordageddon_g16.db.contracts.DAO;

public interface Repository {
    <T,TDAO extends DAO<T>> TDAO getDAO(String category);
    void close();
}
