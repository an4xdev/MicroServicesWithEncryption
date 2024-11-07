package Database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseDriver {

    private static Connection conn;
    private DatabaseDriver(){}

    private static void connect() throws SQLException {
        String url = "jdbc:postgresql://localhost/servicedb";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "P@ssword123");
        conn = DriverManager.getConnection(url, props);
    }

    public static Connection getConnection() throws SQLException {
        if(conn == null){
            connect();
        }
        return conn;
    }
}
