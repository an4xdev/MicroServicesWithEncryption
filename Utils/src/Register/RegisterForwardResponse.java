package Register;

import java.io.Serializable;

public class RegisterForwardResponse implements Serializable {
    public int code;
    public String message;
    public int userId;

    public static String ConvertToString(RegisterForwardResponse response){
        return String.format("%d;%s;%d", response.code, response.message, response.userId);
    }

    public static RegisterForwardResponse ConvertFromString(String data){
        RegisterForwardResponse response = new RegisterForwardResponse();
        String[] list = data.split(";");
        response.code = Integer.parseInt(list[0]);
        response.message = list[1];
        response.userId = Integer.parseInt(list[2]);
        return response;
    }
}
