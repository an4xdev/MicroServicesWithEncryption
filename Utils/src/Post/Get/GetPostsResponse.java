package Post.Get;

import Messages.Message;

public class GetPostsResponse extends Message {
    public GetPostsResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
