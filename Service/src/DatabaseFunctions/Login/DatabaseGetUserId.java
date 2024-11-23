package DatabaseFunctions.Login;

import Database.DatabaseDriver;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseGetUserId {
    public static int getUserId(String userName, PublicKey publicKey) {
        Connection connection;
        try {
            connection = DatabaseDriver.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("SELECT users.id FROM users WHERE login = ? AND pub_key = ?");
            statement.setString(1, userName);
            statement.setString(2, publicKey.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        int user_id = -1;
        if(resultSet != null) {
            try {
                resultSet.next();
                user_id = resultSet.getInt("id");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
       return user_id;
    }
}
