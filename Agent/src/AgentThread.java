import Agent.AgentMessage;
import Agent.Requests.*;
import Agent.Responses.ClosedConnection;
import Agent.Responses.ConnectData;
import Agent.Responses.ReadyToClose;
import Enums.Services;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentThread extends Thread {

    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final AtomicInteger port;
    private final HashMap<Services, ArrayList<ServiceInstance>> services;
    private final int agentPort;
    private final String agentHost;
    private final ArrayDeque<AgentMessage> globalMessages;
    private final ArrayList<ConnectionBetweenServices> connections;
    private String serviceName;
    private UUID serviceId;
    private boolean isRunning = true;

    public AgentThread(Socket clientSocket,
                       HashMap<Services, ArrayList<ServiceInstance>> services,
                       AtomicInteger port,
                       ArrayDeque<AgentMessage> globalMessages,
                       int agentPort, String agentHost,
                       ArrayList<ConnectionBetweenServices> connections) {
        this.clientSocket = clientSocket;
        this.services = services;
        this.port = port;
        this.globalMessages = globalMessages;
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        this.connections = connections;
        serviceId = null;
        prepare();
        start();
    }

    private void prepare() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void cleanResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public synchronized void sendMessage(AgentMessage message) {
        try {
            out.writeObject(message);
        } catch (IOException e) {
            Utils.logException(e, "Error while sending message.");
            isRunning = false;
        }
    }

    private void handleHelloMessage(HelloMessage message) {
        Utils.logInfo("Received hello message.");
        synchronized (globalMessages) {
            globalMessages.add(message);
            globalMessages.notify();
        }
        serviceName = message.serviceName;
        serviceId = message.serviceId;
    }

    private void handleRegisterToAgent(RegisterToAgent req) {
        Utils.logInfo("Received register to agent message.");
        serviceName = req.serviceName;
        serviceId = req.serviceId;
    }

    private void handleConnectToService(ConnectToService req) {
        Utils.logInfo("Received connect to service message.");
        int servicePort = -1;
        synchronized (services) {
            if (services.containsKey(req.service)) {
                var instances = services.get(req.service);
                if (instances != null) {
                    var instance = instances.getFirst();
                    if (instance != null) {
                        Utils.logInfo("Service instance found.");
                        var response = new ConnectData(req.messageId, "localhost", instance.getPort(), instance.getServiceId(), instance.getServiceName());
                        try {
                            out.writeObject(response);
                        } catch (IOException e) {
                            Utils.logException(e, "Error while sending response.");
                        }
                    }
                }
            } else {
                synchronized (port) {
                    servicePort = port.incrementAndGet();
                    port.notifyAll();
                }
                Utils.logInfo("Service instance not found. Creating new one.");
//                var serviceInstance =
                        new ServiceInstance(servicePort, req.service, agentPort, agentHost, req.messageId, UUID.randomUUID()).start();
//                serviceInstance.start();
//                services.put(req.service, new ArrayList<>() {{
//                    add(serviceInstance);
//                }});
            }
            services.notifyAll();
            AgentMessage message;

            Utils.logDebug("Waiting for hello message.");
            while (true) {
                synchronized (globalMessages) {
                    message = globalMessages.poll();
                    if (message == null) {
                        continue;
                    }
                    if (message.messageId.equals(req.messageId) && message instanceof HelloMessage) {
                        Utils.logDebug("Found message in global messages.");
                        break;
                    }
                    globalMessages.push(message);
                    globalMessages.notify();
                }
            }
            Utils.logDebug("Hello message received.");

            HelloMessage helloMessage = (HelloMessage) message;
            ConnectData response = new ConnectData(req.messageId, "localhost", servicePort, helloMessage.serviceId, helloMessage.serviceName);

            Utils.logInfo("Sending response.");
            try {
                out.writeObject(response);
            } catch (IOException e) {
                Utils.logException(e, "Error while sending response.");
            }
        }

    }

    private void handleCreatedConnection(CreatedConnection req) {
        Utils.logInfo("Received created connection message.");
        synchronized (connections) {
            connections.add(
                    new ConnectionBetweenServices(
                            req.sourceService,
                            req.sourceServiceId,
                            req.targetService,
                            req.targetServiceId)
            );
            connections.notifyAll();
        }
    }

    private void handleSentData(SentData req) {
        Utils.logInfo("Received sent data message.");
        synchronized (connections) {
            for (var conn : connections) {
                if (conn.getTargetServiceId() == req.serviceId) {
                    Utils.logInfo("Updating connection activity.");
                    conn.updateActivity();
                }
            }
            connections.notifyAll();
        }
        synchronized (services) {
            for (var instances : services.values()) {
                for (var instance : instances) {
                    if (instance.getServiceId().equals(req.serviceId)) {
                        Utils.logInfo("Updating service activity.");
                        instance.updateLastUsed();
                    }
                }
            }
            services.notifyAll();
        }
    }

    private void handleReadyToClose(ReadyToClose req) {
        Utils.logInfo("Received ready to close message.");
        synchronized (services) {
            for (var instances : services.values()) {
                instances.removeIf(instance -> instance.getServiceId().equals(req.serviceId));
            }
            services.notifyAll();
        }
    }

    private void handleClosedConnection(ClosedConnection req) {
        Utils.logInfo("Received closed connection message.");
        synchronized (connections) {
            connections.removeIf(conn ->
                    conn.getTargetServiceId() == req.targetServiceId &&
                            conn.getSourceServiceId() == req.sourceServiceId
            );
            connections.notifyAll();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            Object receivedObject;
            try {
                receivedObject = in.readObject();
                if (receivedObject == null) {
                    break;
                }
            } catch (IOException e) {
                if (e instanceof EOFException) {
                    Utils.logInfo("Connection closed by client.");
                } else {
                    Utils.logException(e, "Input/Output operations failed.");
                }
                break;
            } catch (ClassNotFoundException e) {
                Utils.logException(e, "Unknown message.");
                break;
            }

            if (receivedObject instanceof AgentMessage) {
                switch (receivedObject) {
                    case HelloMessage message -> handleHelloMessage(message);
                    case RegisterToAgent req -> handleRegisterToAgent(req);
                    case ConnectToService req -> handleConnectToService(req);
                    case CreatedConnection req -> handleCreatedConnection(req);
                    case SentData req -> handleSentData(req);
                    case ReadyToClose req -> handleReadyToClose(req);
                    case ClosedConnection req -> handleClosedConnection(req);
                    default -> Utils.logError("Unknown message type.");
                }
            } else {
                Utils.logError("Unknown data.");
            }
        }

        cleanResources();
    }
}
