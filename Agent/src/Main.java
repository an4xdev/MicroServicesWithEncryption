import Agent.AgentMessage;
import Enums.Ports;
import Enums.Services;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.Provider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        ArrayDeque<AgentMessage> globalMessages = new ArrayDeque<>();
        HashMap<Services, ArrayList<ServiceInstance>> services = new HashMap<>();
        AtomicInteger port = new AtomicInteger(Ports.Agent.getPort());
        int agentPort = Ports.Agent.getPort();
        String agentHost = "localhost";
        try (ServerSocket serverSocket = new ServerSocket(port.get())) {
            Utils.logInfo("Agent started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utils.logDebug("Connection established with client");
                Utils.logDebug("Client port: " + clientSocket.getPort());
                new Thread(new AgentThread(clientSocket, services, port, globalMessages ,agentPort, agentHost)).start();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while creating server socket");
        }
    }
}