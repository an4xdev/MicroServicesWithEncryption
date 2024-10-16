import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        int port = Utils.Ports.ApiGateway.getPort();
        boolean debug = Utils.debug;

        KeyPair keyPair = Utils.generateKeyPair();

        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Api Gateway started on port " + port);
                try {
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        if (debug) {
                            System.out.println("Connection established with client");
                            System.out.println("Client port: " + clientSocket.getPort());
                        }
                        new Thread(new ApiGatewayThread(debug, clientSocket, keyPair)).start();
                    }
                } catch (IOException e) {
                    Utils.logError(debug, e, "Error while creating socket from incoming connection");
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            Utils.logError(true, e, "Error while creating server socket");
        }
    }
}