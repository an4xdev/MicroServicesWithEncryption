import Agent.AgentMessage;
import Enums.Ports;
import Enums.Services;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        ArrayDeque<AgentMessage> globalMessages = new ArrayDeque<>();

        HashMap<Services, ArrayList<ServiceInstance>> services = new HashMap<>();

        ArrayList<ConnectionBetweenServices> connections = new ArrayList<>();

        ArrayList<AgentThread> agentThreads = new ArrayList<>();

        AtomicInteger port = new AtomicInteger(Ports.Agent.getPort());
        int agentPort = Ports.Agent.getPort();
        String agentHost = "localhost";

        CheckServicesActivityThread checkServicesActivityThread = new CheckServicesActivityThread(connections, services, agentThreads);
        new Thread(checkServicesActivityThread).start();

        try (ServerSocket serverSocket = new ServerSocket(port.get())) {
            Utils.logInfo("Agent started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Utils.logDebug("Connection established with client");
                Utils.logDebug("Client port: " + clientSocket.getPort());
                AgentThread agentThread = new AgentThread(clientSocket, services, port, globalMessages, agentPort, agentHost, connections);
                agentThreads.add(agentThread);
//                new Thread(agentThread).start();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while creating server socket");
        }
    }
}