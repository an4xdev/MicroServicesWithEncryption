package DatabaseFunctions.Register;

import Database.DatabaseDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseInsertNewUser {
    public static int insertNewUser(String userName, String publicKey){
        Connection connection;
        try {
            connection = DatabaseDriver.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT INTO users(login, pub_key) VALUES (?, ?)");
            statement.setString(1, userName);
            statement.setString(2, publicKey);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            int result = statement.executeUpdate();
            statement.close();
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
