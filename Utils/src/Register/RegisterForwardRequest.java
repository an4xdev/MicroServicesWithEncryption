package Register;

import Key.KeyUtils;
import Messages.BaseForwardRequest;

import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

public class RegisterForwardRequest extends BaseForwardRequest {
    public String login;
    public PublicKey publicKey;

    public static String ConvertToString(RegisterForwardRequest request) {
        String publicKeyString = Base64.getEncoder().encodeToString(request.publicKey.getEncoded());
        return request.messageId.toString() + ";" + request.login + ";" + publicKeyString;
    }

    public static RegisterForwardRequest ConvertToRegisterForwardRequest(String request) {
        String[] parts = request.split(";");
        RegisterForwardRequest newRequest = new RegisterForwardRequest();
        newRequest.messageId = UUID.fromString(parts[0]);
        newRequest.login = parts[1];
        newRequest.publicKey = KeyUtils.getPublicKeyFromString(parts[2]);
        return newRequest;
    }
}
