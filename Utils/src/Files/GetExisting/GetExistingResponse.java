package Files.GetExisting;

import Messages.Message;

public class GetExistingResponse extends Message {
    public GetExistingResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
