package Model;

import java.io.Serializable;

public class PostModel implements Serializable {
    public int userId;
    public String usesLogin;
    public String message;

    public PostModel(int userId, String usesLogin, String message) {
        this.userId = userId;
        this.usesLogin = usesLogin;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("%d_%s_%s", userId, usesLogin, message);
    }

    public static PostModel ConvertFromString(String data){
        String[] list = data.split("_");
        return new PostModel(Integer.parseInt(list[0]), list[1], list[2]);
    }
}
