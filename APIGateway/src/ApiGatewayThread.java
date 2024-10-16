import Register.RegisterRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;

public class ApiGatewayThread implements Runnable {
    private final Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private final boolean debug;
    private final KeyPair keyPair;
    private PublicKey clientPublicKey;

    public ApiGatewayThread(boolean debug, Socket socket, KeyPair keyPair) {
        this.debug = debug;
        this.socket = socket;
        this.keyPair = keyPair;
    }

    private void prepare() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            Utils.logError(debug, e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void initialConnection() {
        try {
            outputStream.writeObject(keyPair.getPublic());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (debug) {
            System.out.println("Sent public key to client");
        }

        clientPublicKey = null;
        try {
            clientPublicKey = (PublicKey) inputStream.readObject();
        } catch (IOException e) {
            if (e instanceof EOFException) {
                System.out.println("Connection closed by client.");
            } else {
                Utils.logError(debug, e, "Input/Output operations failed.");
            }
            cleanResources();
        } catch (ClassNotFoundException e) {
            Utils.logError(debug, e, "Unknown message.");
            cleanResources();
        }
        if (debug) {
            System.out.println("Got client public key");
        }
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
            Utils.logError(debug, e, "Error while closing resources.");
            System.exit(-2);
        }
    }

    @Override
    public void run() {
        prepare();
        initialConnection();

        Object receivedObject;
        while (true) {
            try {
                if ((receivedObject = inputStream.readObject()) == null) break;
            } catch (IOException e) {
                if (e instanceof EOFException) {
                    System.out.println("Connection closed by client.");
                } else {
                    Utils.logError(debug, e, "Input/Output operations failed.");
                }
                break;
            } catch (ClassNotFoundException e) {
                Utils.logError(debug, e, "Unknown message.");
                break;
            }
            
            if (receivedObject instanceof RegisterRequest req) {
                if (debug) {
                    System.out.println("Got register request");
                }
                byte[] dataWithSymmetricKey = req.userName();
                byte[] fingerprintWithSymmetricKey = req.fingerPrint();
                byte[] encryptedSymmetricKey = req.encryptedSymmetricKey();

                SecretKey symmetricKey = null;
                try {
                    symmetricKey = Utils.decryptKey(encryptedSymmetricKey, keyPair.getPrivate());
                } catch (Exception e) {
                    Utils.logError(debug, e, "Could not decrypt symmetric key.");
                    break;
                }

                byte[] data;
                try {
                    data = Utils.decrypt(dataWithSymmetricKey, symmetricKey);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    Utils.logError(debug, e, "Could not decrypt data.");
                    break;
                }

                byte[] fingerprint;
                try {
                    fingerprint = Utils.decrypt(fingerprintWithSymmetricKey, symmetricKey);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    Utils.logError(debug, e, "Could not decrypt fingerprint.");
                    break;
                }

                byte[] hashedData;
                try {
                    hashedData = Utils.hashData(new String(data));
                } catch (NoSuchAlgorithmException e) {
                    Utils.logError(debug, e, "Could not hash data.");
                    break;
                }

                boolean isSignatureValid;
                try {
                    isSignatureValid = Utils.verifySignature(hashedData, fingerprint, clientPublicKey);
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    Utils.logError(debug, e, "Could not verify signature.");
                    break;
                }

                if (isSignatureValid) {
                    System.out.println("Signature is valid. Message is authentic.");
                    System.out.println("Decrypted message: " + new String(data));
                } else {
                    System.out.println("Signature is invalid! Message could have been modified.");
                }
            } else {
                System.out.println("Unknown message.");
            }
        }

        cleanResources();
    }
}
