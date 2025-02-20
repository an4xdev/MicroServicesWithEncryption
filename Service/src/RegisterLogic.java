import DatabaseFunctions.Register.DatabaseCheckExistingUser;
import DatabaseFunctions.Register.DatabaseInsertNewUser;
import Register.RegisterForwardRequest;
import Register.RegisterForwardResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RegisterLogic implements Runnable {
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public RegisterLogic(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

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

            if (receivedObject instanceof RegisterForwardRequest req) {
                Utils.logDebug("Got register request.");
                RegisterForwardResponse response = new RegisterForwardResponse();
                response.messageId = req.messageId;
                if (DatabaseCheckExistingUser.checkExistingUser(req.login, req.publicKey)) {
                    response.code = 400;
                    response.message = "User with this public key is already registered.";
                    Utils.logDebug("User with this public key is already registered.");
                } else {
                    boolean added = DatabaseInsertNewUser.insertNewUser(req.login, req.publicKey.toString());
                    if(!added) {
                        response.code = 500;
                        response.message = "Error while adding new user.";
                        Utils.logDebug("Error while adding new user.");
                    }
                    else {
                        response.code = 200;
                        Utils.logDebug("New user with login: " + req.login);
                    }
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
