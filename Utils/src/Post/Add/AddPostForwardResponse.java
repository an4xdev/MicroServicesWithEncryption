package Post.Add;

import Messages.BaseForwardResponse;

import java.io.Serializable;
import java.util.UUID;

public class AddPostForwardResponse extends BaseForwardResponse {

    public static String ConvertToString(AddPostForwardResponse response) {
        return String.format("%s;%d;%s", response.messageId.toString(), response.code, response.message);
    }

    public static AddPostForwardResponse ConvertFromString(String data) {
        AddPostForwardResponse response = new AddPostForwardResponse();
        String[] list = data.split(";");
        response.messageId = UUID.fromString(list[0]);
        response.code = Integer.parseInt(list[1]);
        response.message = list[2];
        return response;
    }
}
