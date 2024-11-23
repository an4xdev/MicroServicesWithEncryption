package Post.Add;

import Messages.Message;

public class AddPostRequest extends Message {
    public AddPostRequest(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
