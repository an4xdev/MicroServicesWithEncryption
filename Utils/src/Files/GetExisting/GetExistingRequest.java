package Files.GetExisting;

import Messages.Message;

public class GetExistingRequest extends Message {
    public GetExistingRequest(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
