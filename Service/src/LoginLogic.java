import DatabaseFunctions.Login.DatabaseGetUserId;
import DatabaseFunctions.Register.DatabaseCheckExistingUser;
import Login.LoginForwardRequest;
import Login.LoginForwardResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class LoginLogic implements Runnable{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public LoginLogic(ObjectOutputStream out, ObjectInputStream in) {
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

            if (receivedObject instanceof LoginForwardRequest req) {
                Utils.logDebug("Got login request.");
                LoginForwardResponse response = new LoginForwardResponse();
                response.messageId = req.messageId;
                if (DatabaseCheckExistingUser.checkExistingUser(req.login, req.publicKey)) {
                    response.userId = DatabaseGetUserId.getUserId(req.login, req.publicKey);
                    response.code = 200;
                    response.message = "Found user in database";
                    Utils.logDebug("User with ID: " + response.userId);
                } else {
                    response.userId = -1;
                    response.code = 400;
                    response.message = "No user in database.";
                    Utils.logDebug("No user in database: "+ req.login + ", " + req.publicKey);
                }

                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    Utils.logException(e, "Input/Output operations failed.");
                    break;
                }

            } else {
                System.out.println("Unknown message.");
            }
        }
    }
}
