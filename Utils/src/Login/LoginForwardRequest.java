package Login;

import Key.KeyUtils;
import Messages.BaseForwardRequest;

import java.security.PublicKey;
import java.util.UUID;

public class LoginForwardRequest extends BaseForwardRequest {
    public String login;
    public PublicKey publicKey;

    public static String ConvertToString(LoginForwardRequest request) {
        return request.messageId.toString()+";" + request.login + ";" + request.publicKey.toString();
    }

    public static LoginForwardRequest ConvertFromString(String request) {
        String[] parts = request.split(";");
        LoginForwardRequest newRequest = new LoginForwardRequest();
        newRequest.messageId = UUID.fromString(parts[0]);
        newRequest.login = parts[1];
        newRequest.publicKey = KeyUtils.getPublicKeyFromString(parts[2]);
        return newRequest;
    }
}
