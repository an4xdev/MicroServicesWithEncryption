import DatabaseFunctions.Posts.DatabaseGetLastTenPosts;
import Post.Get.GetPostsForwardRequest;
import Post.Get.GetPostsForwardResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PostLogic implements Runnable{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public PostLogic(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        Object receivedObject;
        while (true) {
            try {
                if ((receivedObject = in.readObject()) == null) break;
            } catch (IOException e) {
                if (e instanceof EOFException) {
                    Utils.logInfo("Connection closed by client.");
                } else {
                    Utils.logException(e, "Input/Output operations failed.");
                }
                break;
            } catch (ClassNotFoundException e) {
                Utils.logException(e, "Unknown message.");
                break;
            }

            if(receivedObject instanceof GetPostsForwardRequest req){
                Utils.logDebug("Got retrieving posts request.");
                GetPostsForwardResponse response = new GetPostsForwardResponse();
                var posts = DatabaseGetLastTenPosts.GetPosts();
//                TODO: add operation model and process exceptions
                response.messageId = req.messageId;
                response.code = 200;
                response.message = "Count of posts: " + posts.size();
                response.posts = posts;

                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    Utils.logException(e, "Input/Output operations failed.");
                    break;
                }
            }
            else {
                System.out.println("Unknown message.");
            }
        }
    }
}
