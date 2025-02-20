import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {

        int port;
        String name;
        int type;
        int agentPort;
        String agentHost;
        UUID messageId;
        UUID serviceId = null;

        type = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        agentPort = Integer.parseInt(args[2]);
        agentHost = args[3];
        messageId = UUID.fromString(args[4]);
        serviceId = UUID.fromString(args[5]);

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

        ArrayList<ServiceThread> serviceThreads = new ArrayList<>();

        Utils.logInfo("Connecting to agent on port: " + agentPort);

//        ServiceToAgentThread serviceToAgentThread =
                new ServiceToAgentThread(agentPort, agentHost, messageId, name, serviceId, serviceThreads).start();

        Utils.logInfo(name + " service is running on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                Utils.logInfo("New client connected");
                var serviceThread = new ServiceThread(socket, type);
                serviceThreads.add(serviceThread);

                new Thread(serviceThread).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}