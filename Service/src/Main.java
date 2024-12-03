import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        if(args.length < 5) {
            Utils.logDebug("Usage: java Main <type> <port> <agentPort> <agentHost> <uuid>");
            return;
        }

        int port = -1;
        String name = null;
        int type = -1;
        int agentPort = -1;
        String agentHost = null;
        UUID messageId = null;

        type = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        agentPort = Integer.parseInt(args[2]);
        agentHost = args[3];
        messageId = UUID.fromString(args[4]);

        switch (type) {
            case 0 -> name = "Register";
            case 1 -> name = "Login";
            case 2 -> name = "Chat";
            case 3 -> name = "Posts";
            case 4 -> name = "FileServer";
            default -> {
                Utils.logDebug("Unknown option");
                return;
            }
        }

        // TODO: add co threads to handle communication with agent
        ServiceToAgentThread serviceToAgentThread = new ServiceToAgentThread(agentPort, agentHost, messageId, name);

        Utils.logInfo(name + " service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ServiceThread(socket, type)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}