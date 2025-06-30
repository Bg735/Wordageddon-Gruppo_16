package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class JdbcDAO<T> implements DAO<T>{

    protected final Connection connection;

    protected JdbcDAO(Connection connection) {
        this.connection = connection;
    }

    protected <R> R executeQuery(String sql, Callback<ResultSet,R> cb, Object... params) {
        try(var stm=connection.prepareStatement(sql)) {
            if (params.length > 0)
                for (int i = 0; i < params.length; i++)
                    stm.setObject(i + 1, params[i]);
            return cb.call(stm.executeQuery());
        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    protected <R> R executeQuery(String sql, Callback<ResultSet,R> cb) {
        try (var stm = connection.createStatement()) {
            return cb.call(stm.executeQuery(sql));
        } catch (SQLException e) {
            SystemLogger.log("Error trying to execute query: " + sql, e);
            throw new QueryFailedException(e.getMessage());
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

    protected boolean isEmpty(String tableName) {
        String query = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (var stm = connection.createStatement();
             var res = stm.executeQuery(query)) {
            return res.next();
        } catch (SQLException e) {
            SystemLogger.log("Error checking if " + tableName + "has any rows", e);
            return false;
        }
    }
}
