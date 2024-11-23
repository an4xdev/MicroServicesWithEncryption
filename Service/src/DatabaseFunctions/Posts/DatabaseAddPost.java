package DatabaseFunctions.Posts;

import Database.DatabaseDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseAddPost {
    public static boolean addPost(int user_id,String post) {
        Connection connection;
        try {
            connection = DatabaseDriver.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT INTO posts(user_id, message) VALUES (?, ?)");
            statement.setInt(1, user_id);
            statement.setString(2, post);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        int rows_inserted = -1;
        try {
            rows_inserted = statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return rows_inserted == 1;
    }
}
