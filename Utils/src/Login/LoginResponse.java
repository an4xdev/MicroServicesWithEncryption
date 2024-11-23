package Login;

import Messages.Message;

public class LoginResponse extends Message {
    public LoginResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
