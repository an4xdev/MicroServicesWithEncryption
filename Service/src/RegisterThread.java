import Register.RegisterForwardRequest;
import Register.RegisterForwardResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class RegisterThread implements Runnable {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public RegisterThread(Socket socket) {
        this.socket = socket;
    }

    private void prepare() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void cleanResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    @Override
    public void run() {
        prepare();
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
                if(TempDatabaseService.userExist(req.publicKey)){
                    response.userId = -1;
                    response.code = 400;
                    response.message = "User with this public key is already registered.";
                    Utils.logDebug("User with this public key is already registered.");
                }
                else
                {
                    TempDatabaseService.addToDB(req.login, req.publicKey);
                    response.code = 200;
                    response.userId = TempDatabaseService.getID();
                    Utils.logDebug("New user with ID: " + response.userId);
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

        cleanResources();
    }
}
