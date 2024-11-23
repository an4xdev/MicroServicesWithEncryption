package Post.Add;

import java.io.Serializable;
import java.util.UUID;

public class AddPostForwardRequest implements Serializable {
    public UUID messageId;
    public int userId;
    public String post;

    public static String ConvertToString(AddPostForwardRequest request){
        return String.format("%s;%d;%s", request.messageId.toString() ,request.userId, request.post);
    }

    public static AddPostForwardRequest ConvertFromString(String data){
        AddPostForwardRequest request = new AddPostForwardRequest();
        String[] list = data.split(";");
        request.messageId = UUID.fromString(list[0]);
        request.userId = Integer.parseInt(list[1]);
        request.post = list[2];
        return request;
    }
}
