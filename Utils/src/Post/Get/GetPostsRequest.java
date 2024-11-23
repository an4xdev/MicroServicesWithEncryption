package Post.Get;

import Messages.Message;

public class GetPostsRequest extends Message {
    public GetPostsRequest(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
