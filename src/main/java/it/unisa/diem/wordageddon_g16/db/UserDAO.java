package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Document;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO extends JdbcDAO<User, String> {

    public UserDAO(Connection connection) {
        super(connection);
    }

    @Override
    public Optional<User> selectById(String name) {
        if (name instanceof String username) {
            String query = "SELECT * FROM User WHERE name = ?";
            Callback<ResultSet,Optional<User>> callback = res -> {
                try{
                    if (res != null && res.next()) {
                        User user = new User(
                            res.getString("name"),
                            res.getString("password"),
                            res.getBoolean("isAdmin")
                        );
                        return Optional.of(user);
                    }
                    return Optional.empty();
                } catch (SQLException e) {
                    SystemLogger.log("Error trying to get user with id: "+username, e);
                    throw new QueryFailedException(e.getMessage());
                }
            };
            return executeQuery(query, callback, username);
        }
        return Optional.empty();
    }

    @Override
    public List<User> selectAll() {
        String query = "SELECT * FROM User";
        Callback<ResultSet,List<User>> callback = res -> {
            try {
                var result = new ArrayList<User>();
                if (res == null) {
                    return result;
                }
                while (res.next()) {
                    result.add(new User(
                            res.getString("name"),
                            res.getString("password"),
                            res.getBoolean("isAdmin")
                    ));
                }
                return result;
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get all users", e);
                throw new QueryFailedException(e.getMessage());
            }
        };
        return executeQuery(query, callback);
    }

    @Override
    public void insert(User user) {
        String query = "INSERT INTO User (name, password, isAdmin) VALUES (?, ?, ?)";
        try {
            executeUpdate(query, user.getName(), user.getPassword(), user.isAdmin());
        } catch (SQLException e) {
            SystemLogger.log("Error inserting user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    @Override
    public void update(User user) {
        String query = "UPDATE User SET password = ?, isAdmin = ? WHERE name = ?";
        try {
            executeUpdate(query, user.getPassword(), user.isAdmin(), user.getName());
        } catch (SQLException e) {
            SystemLogger.log("Error updating user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    @Override
    public void delete(User user) {
        String query = "DELETE FROM User WHERE name = ?";
        try {
            executeUpdate(query, user.getName());               // Delete on User also deletes all GameReports associated with the user due to integrity constraints (ON DELETE CASCADE)
        } catch (SQLException e) {
            SystemLogger.log("Error deleting user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}
