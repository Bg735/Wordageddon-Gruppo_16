package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.WDM;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class WdmDAO extends JdbcDAO<WDM> {
    public WdmDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<WDM> selectById(Object id) {
        return Optional.empty();
    }

    @Override
    public List<WDM> selectAll() {
        return List.of();
    }

    @Override
    public void delete(WDM wdm) {

    }

    @Override
    public void update(WDM wdm) {

    }

    @Override
    public void insert(WDM wdm) {

    }
}
