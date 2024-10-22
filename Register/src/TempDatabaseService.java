import java.security.PublicKey;
import java.util.ArrayList;

public class TempDatabaseService {
    private static ArrayList<String> logins = new ArrayList<>();
    private static ArrayList<PublicKey> keys = new ArrayList<>();

    public static synchronized void addToDB(String login, PublicKey key){
        logins.add(login);
        keys.add(key);
    }

    public static synchronized boolean userExist(PublicKey key){
        return keys.contains(key);
    }

    public static synchronized int getID(){
        return logins.size() + 1;
    }
}
