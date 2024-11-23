package Post.Get;

import java.io.Serializable;
import java.util.UUID;

public class GetPostsForwardRequest implements Serializable {
    public UUID messageId;

    public static GetPostsForwardRequest ConvertFromString(String data) {
        GetPostsForwardRequest request = new GetPostsForwardRequest();
        request.messageId = UUID.fromString(data);
        return request;
    }

    public static String ConvertToString(GetPostsForwardRequest request) {
        return request.messageId.toString();
    }
}
