package Register;

import Messages.Message;

import java.util.Optional;

public class RegisterRequest extends Message {
    public RegisterRequest(byte[] userName, byte[] fingerPrint, byte[] encryptedSymmetricKey) {
        super(userName, fingerPrint, encryptedSymmetricKey);
    }
}
