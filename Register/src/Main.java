import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = Utils.Ports.Register.getPort();
        Utils.logInfo("Register service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true){
                Socket socket = serverSocket.accept();
                new Thread(new RegisterThread(socket)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}