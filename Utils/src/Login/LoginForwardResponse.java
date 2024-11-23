package Login;

import java.io.Serializable;
import java.util.UUID;

public class LoginForwardResponse implements Serializable {
    public UUID messageId;
    public int code;
    public String message;
    public int userId;

    public static String ConvertToString(LoginForwardResponse response){
        return String.format("%s;%d;%s;%d", response.messageId.toString(), response.code, response.message, response.userId);
    }

    public static LoginForwardResponse ConvertFromString(String data){
        LoginForwardResponse response = new LoginForwardResponse();
        String[] list = data.split(";");
        response.messageId = UUID.fromString(list[0]);
        response.code = Integer.parseInt(list[1]);
        response.message = list[2];
        response.userId = Integer.parseInt(list[3]);
        return response;
    }
}
