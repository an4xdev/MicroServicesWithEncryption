import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class Main {

    private static final String FILE_PATH = "keys.txt";

    public static void main(String[] args) throws NoSuchAlgorithmException {
        int port = Utils.Ports.ApiGateway.getPort();

        KeyPair keyPair = null;
        if (Files.exists(Paths.get(FILE_PATH))) {
            keyPair = readKeyPairFromFile();
            if (keyPair == null) {
                return;
            }
        } else {
            try {
                keyPair = Utils.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                Utils.logException(e, "Could not generate key pair.");
                System.exit(Utils.Codes.KeyError.ordinal());
                return;
            }
            if (keyPair != null) {
                if(!saveKeyPairToFile(keyPair)) {
                    return;
                }
            }
        }

        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                Utils.logInfo("Api Gateway started on port " + port);
                try {
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        Utils.logDebug("Connection established with client");
                        Utils.logDebug("Client port: " + clientSocket.getPort());
                        new Thread(new ApiGatewayThread(clientSocket, keyPair)).start();
                    }
                } catch (IOException e) {
                    Utils.logException(e, "Error while creating socket from incoming connection");
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            Utils.logException(e, "Error while creating server socket");
        }
    }

    private static boolean saveKeyPairToFile(KeyPair keyPair) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(keyPair);
        } catch (IOException e) {
            Utils.logException(e, "Some error happened in application initialization.");
            return false;
        }
        return true;
    }

    private static KeyPair readKeyPairFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (KeyPair) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Utils.logException(e, "Some error happened in application initialization.");
            return null;
        }
    }
}