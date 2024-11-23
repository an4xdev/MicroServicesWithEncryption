package Login;

import Messages.Message;

public class LoginRequest extends Message {
    public LoginRequest(byte[] userName, byte[] fingerPrint) {
        super(userName, fingerPrint);
    }
}
