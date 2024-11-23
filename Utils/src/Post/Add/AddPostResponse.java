package Post.Add;

import Messages.Message;

public class AddPostResponse extends Message {
    public AddPostResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
