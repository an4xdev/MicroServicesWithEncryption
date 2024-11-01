package Register;

import Messages.Message;

import java.util.Optional;

public class RegisterRequest extends Message {
    public RegisterRequest(byte[] userName, byte[] fingerPrint) {
        super(userName, fingerPrint);
    }
}
