package it.unisa.diem.wordageddon_g16.db;

import it.unisa.diem.wordageddon_g16.models.Difficulty;
import it.unisa.diem.wordageddon_g16.models.GameReport;
import it.unisa.diem.wordageddon_g16.models.JdbcRepository;
import it.unisa.diem.wordageddon_g16.models.User;
import it.unisa.diem.wordageddon_g16.services.SystemLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class GameReportDAO extends JdbcDAO<GameReport> {
    public GameReportDAO(Connection conn) {
        super(conn);
    }

    @Override
    public Optional<GameReport> selectById(Object id) {
        if (id instanceof Long reportId) {
            String query = "SELECT * FROM GameReport WHERE id = ?";
            try(var res = executeQuery(query, reportId)){
                if (res != null && res.next()) {
                    var userDAO=JdbcRepository.getInstance().getDAO("user");
                    var user = userDAO.selectById(res.getString("user"));
                    if (user.isPresent())
                        return Optional.of(new GameReport(
                            res.getLong("id"),
                            (User) user.get(),
                            res.getTimestamp("timestamp").toLocalDateTime(),
                            Difficulty.valueOf(res.getString("difficulty")),
                            Duration.parse(res.getString("max_time")),
                            Duration.parse(res.getString("used_time")),
                            res.getInt("question_count"),
                            res.getInt("score")
                        ));
                }
            } catch (SQLException e) {
                SystemLogger.log("Error trying to get report with id: "+reportId, e);
                throw new QueryFailedException(e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GameReport> selectAll() {
        var result = new java.util.ArrayList<GameReport>();
        var userDAO = JdbcRepository.getInstance().getDAO("user");
        String query = "SELECT * FROM GameReport";
        try(var res = executeQuery(query)){
            if (res == null) {
                return result;
            }
            while (res.next()) {
                var user = userDAO.selectById(res.getString("user"));
                if (user.isPresent()) {
                    result.add(new GameReport(
                        res.getLong("id"),
                        (User) user.get(),
                        res.getTimestamp("timestamp").toLocalDateTime(),
                        Difficulty.valueOf(res.getString("difficulty")),
                        Duration.parse(res.getString("max_time")),
                        Duration.parse(res.getString("used_time")),
                        res.getInt("question_count"),
                        res.getInt("score")
                    ));
                }
            }
        } catch (SQLException e) {
            SystemLogger.log("Error trying to get all game reports", e);
            throw new QueryFailedException(e.getMessage());
        }
        return result;
    }

    @Override
    public void insert(GameReport gameReport) {
        String query = "INSERT INTO GameReport (user, timestamp, difficulty, max_time, used_time, question_count, score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            executeUpdate(query,
                gameReport.getUser().getName(),
                gameReport.getTimestamp(),
                gameReport.getDifficulty().name(),
                gameReport.getMaxTime().toString(),
                gameReport.getUsedTime().toString(),
                gameReport.getQuestionCount(),
                gameReport.getScore()
            );
        } catch (SQLException e) {
            SystemLogger.log("Error trying to insert game report", e);
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void update(GameReport gameReport) {
        String query = "UPDATE GameReport SET user = ?, timestamp = ?, difficulty = ?, max_time = ?, used_time = ?, question_count = ?, score = ? WHERE id = ?";
        try {
            executeUpdate(query,
                gameReport.getUser().getName(),
                gameReport.getTimestamp(),
                gameReport.getDifficulty().name(),
                gameReport.getMaxTime().toString(),
                gameReport.getUsedTime().toString(),
                gameReport.getQuestionCount(),
                gameReport.getScore(),
                gameReport.getId()
            );
        } catch (SQLException e) {
            SystemLogger.log("Error trying to update game report with id: " + gameReport.getId(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }

    @Override
    public void delete(GameReport gameReport) {
        String query = "DELETE FROM GameReport WHERE id = ?";
        try {
            executeUpdate(query, gameReport.getId());
        } catch (SQLException e) {
            SystemLogger.log("Error trying to delete game report with id: " + gameReport.getId(), e);
            throw new UpdateFailedException(e.getMessage());
        }
    }
}
