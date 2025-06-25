package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface DAO<T>{
    Optional<T> selectById(Object id);
    List<T> selectAll();
    void insert(T t);
    void update(T t);
    void delete(T t);
}

