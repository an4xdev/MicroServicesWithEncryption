import Checking.Ping;
import Checking.Pong;
import Register.RegisterForwardRequest;
import Register.RegisterForwardResponse;
import Register.RegisterRequest;
import Register.RegisterResponse;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;

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
        Object objRequest = null;
        try {
            objRequest = clientInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new Operation(false, "Could not read object.");
        }
        if (objRequest instanceof Ping ping) {
            Utils.logDebug("Got ping request");
            byte[] dataWithSymmetricKey = ping.data;
            byte[] fingerprintWithSymmetricKey = ping.fingerPrint;
            if (ping.encryptedSymmetricKey == null) {
                return new Operation(false, "Encrypted secret key is empty.");
            }
            byte[] encryptedSymmetricKey = ping.encryptedSymmetricKey;

            if (symmetricKey == null) {
                try {
                    symmetricKey = Utils.decryptKey(encryptedSymmetricKey, keyPair.getPrivate());
                } catch (Exception e) {
                    return new Operation(false, "Could not decrypt symmetric key.");
                }
            }

            var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, keyPair.getPrivate(), clientPublicKey, symmetricKey);
            if (!operation.isSuccessful()) {
                Utils.logDebug(operation.message());
                System.out.println(operation.message());
                return new Operation(false, operation.message());
            }

            String data = operation.message();
            int value;
            try {
                value = Integer.parseInt(data);
            } catch (Exception e) {
                Utils.logError("Could not parse ping value.");
                return new Operation(false, "Could not parse ping value.");
            }

            SecretKey symmetricKey = null;
            try {
                symmetricKey = Utils.decryptKey(encryptedSymmetricKey, keyPair.getPrivate());
            } catch (Exception e) {
                return new Operation(false, "Could not decrypt symmetric key.");
            }

            value += 10;
            var responseOperation = Utils.sendMessage(Pong.class, clientOutputStream, Integer.toString(value), keyPair.getPrivate(), clientPublicKey, symmetricKey, false);
            if (!responseOperation.isSuccessful()) {
                Utils.logError("Could not send pong message: " + responseOperation.message());
                return new Operation(false, responseOperation.message());
            }

            return new Operation(true, "Ping pong data validation successful.");
        } else {
            System.out.println("Unknown message.");
        }
        return new Operation(false, "Unknown message.");
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
            Utils.logError("Could not process ping message: " + pingOperation.message());
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

            if (receivedObject instanceof RegisterRequest req) {
                Utils.logDebug("Got register request");
                byte[] dataWithSymmetricKey = req.data;
                byte[] fingerprintWithSymmetricKey = req.fingerPrint;
                if (req.encryptedSymmetricKey == null && symmetricKey == null) {
                    Utils.logError("Symmetric key is null and encrypted data is empty.");
                    break;
                }

                var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, keyPair.getPrivate(), clientPublicKey, symmetricKey);

                if (!operation.isSuccessful()) {
                    Utils.logDebug(operation.message());
                    System.out.println(operation.message());
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

                if(response == null){
                    Utils.logError("Could not receive response from service.");
                    break;
                }

                var responseOperation = Utils.sendMessage(RegisterResponse.class, clientOutputStream, RegisterForwardResponse.ConvertToString(response), keyPair.getPrivate(), clientPublicKey, symmetricKey, false);

                if(!responseOperation.isSuccessful())
                {
                    Utils.logError(responseOperation.message());
                }

            } else {
                System.out.println("Unknown message.");
            }
        }

        cleanResources();
    }
}
