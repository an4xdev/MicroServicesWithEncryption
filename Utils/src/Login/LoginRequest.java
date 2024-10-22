package Login;

import Messages.Message;

import java.util.Optional;

public class LoginRequest extends Message {
    public LoginRequest(byte[] userName, byte[] fingerPrint, byte[] encryptedSymmetricKey) {
        super(userName, fingerPrint, encryptedSymmetricKey);
    }
}
