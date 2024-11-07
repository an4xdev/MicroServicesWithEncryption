import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Utils.logInfo("Choose your fighter:");
        Utils.logInfo("1. Register");
        Utils.logInfo("2. Login");
        Utils.logInfo("3. Chat");
        Utils.logInfo("4. Post");
        Utils.logInfo("5. Files");
        Utils.logInfo("6. Exit");
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        int port;
        String name;
        switch (choice) {
            case 1:
                port = Utils.Ports.Register.getPort();
                name = "Register";
                break;
            case 2:
                port = Utils.Ports.Login.getPort();
                name = "Login";
                break;
            case 3:
                port = Utils.Ports.Chat.getPort();
                name = "Chat";
            case 4:
                port = Utils.Ports.Posts.getPort();
                name = "Posts";
            case 5:
                port = Utils.Ports.FileServer.getPort();
                name = "FileServer";
            default:
                return;
        }
        Utils.logInfo(name + " service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true){
                Socket socket = serverSocket.accept();
                new Thread(new ServiceThread(socket, choice)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}