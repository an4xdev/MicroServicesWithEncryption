package DatabaseFunctions.Posts;

import Database.DatabaseDriver;
import Model.PostModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseGetLastTenPosts {
    public static ArrayList<PostModel> GetPosts(){
        ArrayList<PostModel> posts = new ArrayList<>();
        Connection connection;
        try {
            connection = DatabaseDriver.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("select posts.message, u.login, u.id from posts join users u on u.id = posts.user_id ORDER BY posts.id DESC LIMIT 10");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                if (!resultSet.next()) break;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            PostModel model = null;
            try {
                model = new PostModel(resultSet.getInt(3), resultSet.getString(2), resultSet.getString(1));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            posts.add(model);
        }

        return posts;
    }
}
