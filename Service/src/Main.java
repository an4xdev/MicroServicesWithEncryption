import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Utils.logInfo("Choose your fighter:");
        Utils.logInfo("1. Register");
        Utils.logInfo("2. Login");
        Utils.logInfo("3. Exit");
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
            default:
                return;
        }
        Utils.logInfo(name + " service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true){
                Socket socket = serverSocket.accept();
                switch (choice) {
                    case 1:
                        new Thread(new RegisterThread(socket)).start();
                        break;
                    case 2:
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}