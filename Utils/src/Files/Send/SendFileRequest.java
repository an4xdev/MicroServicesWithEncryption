package Files.Send;

import Messages.Message;

public class SendFileRequest extends Message {
    public SendFileRequest(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
