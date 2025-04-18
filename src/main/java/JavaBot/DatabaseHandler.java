package JavaBot;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private final Connection connection;

    public DatabaseHandler(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public void addUser(long userId, String username) throws SQLException {
        String query = "INSERT INTO users (user_id, username) VALUES (?, ?) ON CONFLICT (user_id) DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    public int addTask(long userId, String desc, String category, LocalDateTime deadline, int reminderMinutes) throws SQLException {
        String query = "INSERT INTO tasks (user_id, description, category, deadline, reminder_minutes) VALUES (?, ?, ?, ?, ?) RETURNING task_id";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            stmt.setString(2, desc);
            stmt.setString(3, category);
            stmt.setTimestamp(4, Timestamp.valueOf(deadline));
            stmt.setInt(5, reminderMinutes);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("task_id");
            }
            return -1;
        }
    }

    public void markReminderSent(int taskId) throws SQLException {
        String query = "INSERT INTO send_reminders (task_id) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            stmt.executeUpdate();
        }
    }

    public boolean deleteTask(int taskId) throws SQLException {
        String query = "DELETE FROM tasks WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, taskId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Task> getUserTasks(long userId) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT task_id, description, deadline, category, " +
                "is_completed FROM tasks WHERE user_id = ? ORDER BY deadline";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(new Task(rs.getInt("task_id"),
                                    userId,
                                    rs.getString("description"),
                                    rs.getTimestamp("deadline").toLocalDateTime(),
                                    rs.getString("category"),
                                    rs.getBoolean("is_completed")
                ));
            }
        }
        return tasks;
    }

    public List<Task> getUpcomingTasks(int thresholdMinutes) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query = """
            SELECT t.task_id, t.user_id, t.description, t.deadline, t.reminder_minutes 
            FROM tasks t
            LEFT JOIN send_reminders sr ON t.task_id = sr.task_id
            WHERE 
                t.is_completed = FALSE AND
                t.deadline BETWEEN NOW() AND NOW() + INTERVAL '1 MINUTE' * t.reminder_minutes AND
                (sr.task_id IS NULL OR sr.sent_at < NOW() - INTERVAL '1 HOUR')
            """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tasks.add(new Task(
                        rs.getInt("task_id"),
                        rs.getLong("user_id"),
                        rs.getString("description"),
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        null,
                        false
                ));
            }
        }
        return tasks;
    }
}
