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

    private final Map<String, JdbcDAO<?>> daos = new HashMap<>();
    private Connection conn;

    public JdbcRepository() {
        try {
            conn = DriverManager.getConnection(Config.get(Config.Props.DB_URL));
            var userDAO = new UserDAO(conn);
            var documentDAO = new DocumentDAO(conn);
            daos.put("user", userDAO);
            daos.put("document", documentDAO);
            daos.put("stopWord", new StopWordDAO(conn));
            daos.put("gameReport", new GameReportDAO(conn, documentDAO, userDAO));
            daos.put("wdm", new WdmDAO(conn, documentDAO));
        } catch (SQLException e) {
            SystemLogger.log("Could not establish a connection to the database: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T,TDAO extends DAO<T>> TDAO getDAO(String category) {
        if (daos.containsKey(category)) {
            return (TDAO) daos.get(category);
        } else {
            throw new IllegalArgumentException("No DAO found for category: " + category);
        }
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