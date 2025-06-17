package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class JdbcDAO<T> implements DAO<T>{

    protected final Connection connection;

    protected JdbcDAO(Connection connection) {
        this.connection = connection;
    }

    protected ResultSet executeQuery(String sql, Object... params) {
        try(var stm=connection.prepareStatement(sql)) {
            if (params.length > 0)
                for (int i = 0; i < params.length; i++)
                    stm.setObject(i + 1, params[i]);
            return stm.executeQuery(sql);

        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            return null;
        }
    }

    protected ResultSet executeQuery(String sql) {
        try (var stm = connection.createStatement()) {
            return stm.executeQuery(sql);
        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            return null;
        }
    }

    protected void executeUpdate(String sql, Object... params) throws SQLException {
        try (var stm = connection.prepareStatement(sql)) {
            if (params.length > 0)
                for (int i = 0; i < params.length; i++)
                    stm.setObject(i + 1, params[i]);
            stm.executeUpdate();
        }
    }
}
