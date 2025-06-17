package it.unisa.diem.wordageddon_g16.models;

import it.unisa.diem.wordageddon_g16.db.*;
import it.unisa.diem.wordageddon_g16.models.interfaces.Repository;
import it.unisa.diem.wordageddon_g16.services.Config;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class JdbcRepository implements Repository {

    private static JdbcRepository instance;
    private Map<String, DAO<?>> daos;
    private Connection conn;

    private JdbcRepository() {}

    public static void init() {
        instance.daos = new HashMap<>();
        try {
            instance.conn = DriverManager.getConnection(Config.DB_URL);
            instance.daos.put("user", new UserDAO(instance.conn));
            instance.daos.put("gameReport", new GameReportDAO(instance.conn));
            instance.daos.put("wdm", new WdmDAO(instance.conn));
            instance.daos.put("document", new DocumentDAO(instance.conn));
            instance.daos.put("stopWord", new StopWordDAO(instance.conn));
        } catch (SQLException e) {
            SystemLogger.log("Could not establish a connection to the database: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> DAO<T> getDAO(String category) {
        if (daos.containsKey(category)) {
            return (DAO<T>) daos.get(category);
        } else {
            throw new IllegalArgumentException("No DAO found for category: " + category);
        }
    }

    public static JdbcRepository getInstance() {
        if (instance == null) {
            instance = new JdbcRepository();
            init();
        }
        return instance;
    }

    public void close(){
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                SystemLogger.log("Could not close the database connection: ", e);
            }
        }
    }
}
