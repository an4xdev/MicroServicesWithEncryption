import Register.RegisterForwardRequest;
import Register.RegisterForwardResponse;
import Register.RegisterRequest;
import Register.RegisterResponse;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Random;

public class ApiGatewayThread implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream clientInputStream;
    private ObjectOutputStream clientOutputStream;

    private Socket serviceSocket;
    private ObjectInputStream serviceInputStream;
    private ObjectOutputStream serviceOutputStream;

    private final KeyPair keyPair;
    private PublicKey clientPublicKey;
    private SecretKey symmetricKey;

    public ApiGatewayThread(Socket socket, KeyPair keyPair) {
        this.clientSocket = socket;
        this.keyPair = keyPair;
    }

    private void prepare() {
        try {
            clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void initialConnection() {
        try {
            clientOutputStream.writeObject(keyPair.getPublic());
            clientOutputStream.flush();
        } catch (IOException e) {
            cleanResources();
        }
        Utils.logDebug("Sent public key to client");

        clientPublicKey = null;
        try {
            clientPublicKey = (PublicKey) clientInputStream.readObject();
        } catch (IOException e) {
            if (e instanceof EOFException) {
                System.out.println("Connection closed by client.");
            } else {
                Utils.logException(e, "Input/Output operations failed.");
            }
            cleanResources();
        } catch (ClassNotFoundException e) {
            Utils.logException(e, "Unknown message.");
            cleanResources();
        }
        Utils.logDebug("Got client public key");
    }

    private void cleanResources() {
        try {
            if (clientInputStream != null) {
                clientInputStream.close();
            }
            if (clientOutputStream != null) {
                clientOutputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    private Operation processPing() {

        Utils.logDebug("Processing ping pong operation.");
        // got ping from client
        byte[] objRequest = null;
        try {
            objRequest = (byte[]) clientInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new Operation(false, "Could not read object.");
        }

        // decrypting ping (client is checking if server has the private key)

        int value;
        try {
            value = Utils.decryptPing(objRequest, keyPair.getPrivate());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not decrypt ping request.");
        }

        // sending pong to client

        Utils.logDebug("Sending pong response to client.");

        value += 10;

        try {
            clientOutputStream.writeInt(value);
        } catch (IOException e) {
            return new Operation(false, "Could not send pong response.");
        }

        Utils.logDebug("Sending ping request to client.");

        // sending ping to client(API is checking if client has the private key)

        int valueAPI = new Random().nextInt();

        byte[] pingRequestAPI;
        try {
            pingRequestAPI = Utils.encryptPing(valueAPI, clientPublicKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not encrypt ping request.");
        }

        try {
            clientOutputStream.writeObject(pingRequestAPI);
        } catch (IOException e) {
            return new Operation(false, "Could not send ping request to client.");
        }

        // got pong from client

        int pongValueClient;
        try {
            pongValueClient = clientInputStream.readInt();
        } catch (IOException e) {
            return new Operation(false, "Could not read pong response from Client.");
        }

        Utils.logDebug("Got pong response from client.");

        // validating pong from client

        if (pongValueClient - 10 != valueAPI) {
            Utils.logError("Ping pong data validation failed.");
            return new Operation(false, "Ping pong data validation failed.");
        }

        try {
            clientOutputStream.writeInt(200);
            clientOutputStream.flush();
        } catch (IOException e) {
            return new Operation(false, "Could not send final response to client.");
        }

        Utils.logDebug("Ping pong data validation successful.");
        return new Operation(true, "");

    }

    private Operation sendToService(Object obj, int port) {
        try {
            serviceSocket = new Socket("localhost", port);
        } catch (IOException e) {
            return new Operation(false, "Cannot connect to service on port: " + port);
        }

        try {
            serviceOutputStream = new ObjectOutputStream(serviceSocket.getOutputStream());
            serviceInputStream = new ObjectInputStream(serviceSocket.getInputStream());
        } catch (IOException e) {
            return new Operation(false, "Cannot establish connection.");
        }

        try {
            serviceOutputStream.writeObject(obj);
            serviceOutputStream.flush();
        } catch (IOException e) {
            return new Operation(false, "Cannot send message.");
        }

        return new Operation(true, "");
    }

    private <T> T receiveMessage() {
        if (serviceSocket == null || serviceOutputStream == null) {
            Utils.logError("Output objects aren't closed.");
            return null;
        }
        T obj = null;
        try {
            obj = (T) serviceInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Utils.logException(e, "Input/output operations failed.");
        }

        try {
            serviceInputStream.close();
            serviceOutputStream.close();
            serviceSocket.close();
        } catch (IOException e) {
            Utils.logException(e, "Closing connection failed.");
            return null;
        }

        return obj;
    }

    @Override
    public void run() {
        prepare();
        initialConnection();
        var pingOperation = processPing();
        if (!pingOperation.isSuccessful()) {
            Utils.logError("Could not process ping pong operation: " + pingOperation.message());
            cleanResources();
            return;
        }
        Object receivedObject;
        while (true) {
            try {
                if ((receivedObject = clientInputStream.readObject()) == null) break;
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

            if(receivedObject instanceof SymmetricKeyMessage message) {
                try {
                    symmetricKey = Utils.decryptKey(message.key, keyPair.getPrivate());
                } catch (Exception e) {
                    Utils.logException(e, "Could not decrypt symmetric key.");
                    break;
                }
                Utils.logDebug("Got symmetric key.");
            }
            else if (receivedObject instanceof RegisterRequest req) {
                Utils.logDebug("Got register request");
                byte[] dataWithSymmetricKey = req.data;
                byte[] fingerprintWithSymmetricKey = req.fingerPrint;

                var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                if (!operation.isSuccessful()) {
                    Utils.logDebug(operation.message());
                    Utils.logInfo(operation.message());
                    break;
                }

                var request = new RegisterForwardRequest();
                request.login = operation.message();
                request.publicKey = clientPublicKey;

                var sendToServiceOperation = sendToService(request, Utils.Ports.Register.getPort());
                if (!sendToServiceOperation.isSuccessful()) {
                    Utils.logError(sendToServiceOperation.message());
                    break;
                }

                RegisterForwardResponse response = receiveMessage();

                if (response == null) {
                    Utils.logError("Could not receive response from service.");
                    break;
                }

                var responseOperation = Utils.sendMessage(RegisterResponse.class, clientOutputStream, RegisterForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                if (!responseOperation.isSuccessful()) {
                    Utils.logError(responseOperation.message());
                }

            } else {
                System.out.println("Unknown message.");
            }
        }

        cleanResources();
    }
}
