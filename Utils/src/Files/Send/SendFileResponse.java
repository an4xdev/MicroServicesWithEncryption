package Files.Send;

import Messages.Message;

public class SendFileResponse extends Message {
    public SendFileResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
