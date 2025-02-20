package Register;

import Messages.Message;

public class RegisterRequest extends Message {
    public RegisterRequest(byte[] userName, byte[] fingerPrint) {
        super(userName, fingerPrint);
    }
}
