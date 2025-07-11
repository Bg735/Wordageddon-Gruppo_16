package it.unisa.diem.wordageddon_g16.db.contracts;

import java.util.Set;

public interface StopWordDAO extends DAO<String> {
    @Override
    Set<String> selectAll();

    boolean isEmpty();
}
