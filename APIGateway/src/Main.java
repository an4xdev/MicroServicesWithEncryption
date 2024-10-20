import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        int port = Utils.Ports.ApiGateway.getPort();

        KeyPair keyPair = Utils.generateKeyPair();

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
}