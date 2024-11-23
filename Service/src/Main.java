import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int port = -1;
        String name = null;
        int choice = -1;
        if (args.length == 1) {
            choice = Integer.parseInt(args[0]);
        } else if (args.length == 0) {
            Utils.logInfo("Choose your fighter:");
            Utils.logInfo("1. Register");
            Utils.logInfo("2. Login");
            Utils.logInfo("3. Chat");
            Utils.logInfo("4. Post");
            Utils.logInfo("5. Files");
            Utils.logInfo("6. Exit");
            Scanner scanner = new Scanner(System.in);
            choice = scanner.nextInt();
        }
        switch (choice) {
            case 1 -> {
                port = Utils.Ports.Register.getPort();
                name = "Register";
            }
            case 2 -> {
                port = Utils.Ports.Login.getPort();
                name = "Login";
            }
            case 3 -> {
                port = Utils.Ports.Chat.getPort();
                name = "Chat";
            }
            case 4 -> {
                port = Utils.Ports.Posts.getPort();
                name = "Posts";
            }
            case 5 -> {
                port = Utils.Ports.FileServer.getPort();
                name = "FileServer";
            }
            default -> {
                Utils.logDebug("Exit / Unknown option");
                return;
            }
        }

        Utils.logInfo(name + " service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ServiceThread(socket, choice)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}