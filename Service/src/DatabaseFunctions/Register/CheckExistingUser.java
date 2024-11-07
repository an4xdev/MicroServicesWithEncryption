package DatabaseFunctions.Register;

import Database.DatabaseDriver;

import java.security.PublicKey;
import java.sql.*;

public class CheckExistingUser {
    public static boolean checkExistingUser(String userName, PublicKey publicKey) {
        Connection connection;
        try {
            connection = DatabaseDriver.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("SELECT * FROM users WHERE login = ? AND pub_key = ?");
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
        int counter = 0;
        while (true) {
            try {
                if (!resultSet.next()) break;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try {
                System.out.println(resultSet.getString("login"));
                counter++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return counter > 0;
    }
}
