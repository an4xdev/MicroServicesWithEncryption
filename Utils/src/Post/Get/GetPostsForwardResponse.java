package Post.Get;

import Messages.BaseForwardResponse;
import Model.PostModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class GetPostsForwardResponse extends BaseForwardResponse {
    public ArrayList<PostModel> posts;

    public static String ConvertToString(GetPostsForwardResponse response){
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s;%d;%s;", response.messageId.toString() ,response.code, response.message));
        response.posts.forEach(p -> builder.append(String.format("%s~", p.toString())));
        return builder.toString();
    }

    public static GetPostsForwardResponse ConvertFromString(String data){
        GetPostsForwardResponse response = new GetPostsForwardResponse();
        response.posts = new ArrayList<>();
        String[] list = data.split(";");
        response.messageId = UUID.fromString(list[0]);
        response.code = Integer.parseInt(list[1]);
        response.message = list[2];

        String[] posts = list[3].split("~");
        for (var p : posts) {
            response.posts.add(PostModel.ConvertFromString(p));
        }
        return response;
    }
}
