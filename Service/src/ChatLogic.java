import DatabaseFunctions.Posts.DatabaseAddPost;
import Post.Add.AddPostForwardRequest;
import Post.Add.AddPostForwardResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatLogic implements Runnable{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ChatLogic(ObjectOutputStream out, ObjectInputStream in) {
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

            if (receivedObject instanceof AddPostForwardRequest req) {
                Utils.logDebug("Got adding post request.");
                AddPostForwardResponse response = new AddPostForwardResponse();
                response.messageId = req.messageId;
                if (DatabaseAddPost.addPost(req.userId, req.post)) {
                    response.code = 200;
                    response.message = "Successfully added new post";
                    Utils.logDebug("New post added from: " + req.userId + ": " + req.post);
                } else {
                    response.code = 400;
                    response.message = "An error occurred in adding new post.";
                    Utils.logDebug("Error in adding new post from: "+ req.userId + ": " + req.post);
                }

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
