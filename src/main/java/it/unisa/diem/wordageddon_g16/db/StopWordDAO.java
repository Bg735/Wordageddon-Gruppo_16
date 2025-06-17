package it.unisa.diem.wordageddon_g16.db;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class StopWordDAO extends JdbcDAO<String> {
    public StopWordDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<String> selectById(Object id) {
        return Optional.empty();
    }

    @Override
    public List<String> selectAll() {
        return List.of();
    }

    @Override
    public void insert(String s) {

    }

    @Override
    public void update(String s) {

    }

    @Override
    public void delete(String s) {

    }
}
