package Register;

import java.io.Serializable;
import java.util.UUID;

public class RegisterForwardResponse implements Serializable {
    public UUID messageId;
    public int code;
    public String message;

    public static String ConvertToString(RegisterForwardResponse response){
        return String.format("%s;%d;%s", response.messageId.toString() ,response.code, response.message);
    }

    public static RegisterForwardResponse ConvertFromString(String data){
        RegisterForwardResponse response = new RegisterForwardResponse();
        String[] list = data.split(";");
        response.messageId = UUID.fromString(list[0]);
        response.code = Integer.parseInt(list[1]);
        response.message = list[2];
        return response;
    }
}
