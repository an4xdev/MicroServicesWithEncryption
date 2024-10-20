import Checking.Ping;
import Checking.Pong;
import Register.RegisterRequest;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;

public class ApiGatewayThread implements Runnable {
    private final Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private final KeyPair keyPair;
    private PublicKey clientPublicKey;

    public ApiGatewayThread(Socket socket, KeyPair keyPair) {
        this.socket = socket;
        this.keyPair = keyPair;
    }

    private void prepare() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void initialConnection() {
        try {
            outputStream.writeObject(keyPair.getPublic());
            outputStream.flush();
        } catch (IOException e) {
            cleanResources();
        }
        Utils.logDebug("Sent public key to client");

        clientPublicKey = null;
        try {
            clientPublicKey = (PublicKey) inputStream.readObject();
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
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }
    
    private Operation processPing(){
        Object objRequest = null;
        try {
            objRequest = inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return  new Operation(false, "Could not read object.");
        }
        if(objRequest instanceof Ping ping)
        {
            Utils.logDebug("Got ping request");
            byte[] dataWithSymmetricKey = ping.numberValue();
            byte[] fingerprintWithSymmetricKey = ping.fingerPrint();
            byte[] encryptedSymmetricKey = ping.encryptedSymmetricKey();
            
            var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, encryptedSymmetricKey, keyPair.getPrivate(), clientPublicKey);
            if(!operation.isSuccessful())
            {
                Utils.logDebug(operation.message());
                System.out.println(operation.message());
                return new Operation(false, operation.message());
            }
            
            String data = operation.message();
            int value;
            try{
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
            var responseOperation = Utils.sendMessage(Pong.class, outputStream, Integer.toString(value), keyPair.getPrivate(), clientPublicKey, symmetricKey);
            if(!responseOperation.isSuccessful())
            {
                Utils.logError("Could not send pong message: " + responseOperation.message());
                return new Operation(false, responseOperation.message());
            }
            
            return new Operation(true, "Ping pong data validation successful.");
        }
        else
        {
            System.out.println("Unknown message.");
        }
        return new Operation(false, "Unknown message.");
    }

    @Override
    public void run() {
        prepare();
        initialConnection();
        var pingOperation = processPing();
        if(!pingOperation.isSuccessful())
        {
            Utils.logError("Could not process ping message: " + pingOperation.message());
            cleanResources();
            return;
        }
        Object receivedObject;
        while (true) {
            try {
                if ((receivedObject = inputStream.readObject()) == null) break;
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
                byte[] dataWithSymmetricKey = req.userName();
                byte[] fingerprintWithSymmetricKey = req.fingerPrint();
                byte[] encryptedSymmetricKey = req.encryptedSymmetricKey();

                var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, encryptedSymmetricKey, keyPair.getPrivate(), clientPublicKey);

                if (!operation.isSuccessful()) {
                    Utils.logDebug(operation.message());
                    System.out.println(operation.message());
                    break;
                }
                
                String data = operation.message();
                
            } else {
                System.out.println("Unknown message.");
            }
        }

        cleanResources();
    }
}
