package it.unisa.diem.wordageddon_g16.db;

import java.util.Collection;
import java.util.Optional;

public interface DAO<T>{
    Optional<T> selectById(Object id);
    Collection<T> selectAll();
    void insert(T t);
    void update(T t);
    void delete(T t);
}

