package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO extends JdbcDAO<User>{

    public UserDAO(Connection connection) {
        super(connection);
    }

    @Override
    public Optional<User> selectById(Object name) {
        if (name instanceof String username) {
            String query = "SELECT * FROM User WHERE name = ?";
            try(var res= executeQuery(query, username)){
                if (res != null && res.next()) {
                    User user = new User(
                        res.getString("name"),
                        res.getString("password"),
                        res.getBoolean("isAdmin")
                    );
                    return Optional.of(user);
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get user with id: "+username, e);
                throw new QueryFailedException(e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public List<User> selectAll() {
        var result = new ArrayList<User>();
        String query = "SELECT * FROM User";
        try(var res= executeQuery(query)){
            if (res == null) {
                return result;
            }
            while (res.next()) {
                result.add( new User(
                        res.getString("name"),
                        res.getString("password"),
                        res.getBoolean("isAdmin")
                ));
            }
        } catch (SQLException e) {
            SystemLogger.log("Error trying to get all users", e);
            throw new QueryFailedException(e.getMessage());
        }
        return result;
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
            executeUpdate(query, user.getName());
        } catch (SQLException e) {
            SystemLogger.log("Error deleting user: " + user.getName(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}
