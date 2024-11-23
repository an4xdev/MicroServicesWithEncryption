import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = 32137;
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            Utils.logInfo("Agent started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utils.logDebug("Connection established with client");
                Utils.logDebug("Client port: " + clientSocket.getPort());
                new Thread(new AgentThread(clientSocket)).start();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while creating server socket");
        }
    }
}