package Register;

import Messages.Message;

public class RegisterResponse extends Message {
    public RegisterResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
