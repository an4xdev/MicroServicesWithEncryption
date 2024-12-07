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

public class AgentThread implements Runnable {

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
        synchronized (globalMessages) {
            globalMessages.add(message);
            globalMessages.notifyAll();
        }
        serviceName = message.serviceName;
        serviceId = message.serviceId;
    }

    private void handleRegisterToAgent(RegisterToAgent req) {
        serviceName = req.serviceName;
        serviceId = req.serviceId;
    }

    private void handleConnectToService(ConnectToService req) {
        int servicePort = -1;
        synchronized (services) {
            if (services.containsKey(req.service)) {
                var instances = services.get(req.service);
                if (instances != null) {
                    var instance = instances.getFirst();
                    if (instance != null) {
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
                    servicePort = port.getAndIncrement();
                    port.notifyAll();
                }
                var serviceInstance = new ServiceInstance(servicePort, req.service, agentPort, agentHost, req.messageId, UUID.randomUUID());
                services.put(req.service, new ArrayList<>() {{
                    add(serviceInstance);
                }});
            }
            services.notifyAll();
        }

        AgentMessage message;

        synchronized (globalMessages) {
            while (true) {
                message = globalMessages.poll();
                if (message == null) {
                    continue;
                }
                if (message.messageId == req.messageId && message instanceof HelloMessage) {
                    break;
                }
                globalMessages.push(message);
            }
            globalMessages.notifyAll();
        }

        HelloMessage helloMessage = (HelloMessage) message;
        ConnectData response = new ConnectData(req.messageId, "localhost", servicePort, helloMessage.serviceId, helloMessage.serviceName);

        try {
            out.writeObject(response);
        } catch (IOException e) {
            Utils.logException(e, "Error while sending response.");
        }
    }

    private void handleCreatedConnection(CreatedConnection req) {
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
        synchronized (connections) {
            for (var conn : connections) {
                if (conn.getTargetServiceId() == req.serviceId) {
                    conn.updateActivity();
                }
            }
            connections.notifyAll();
        }
        synchronized (services) {
            for (var instances : services.values()) {
                for (var instance : instances) {
                    if (instance.getServiceId().equals(req.serviceId)) {
                        instance.updateLastUsed();
                    }
                }
            }
            services.notifyAll();
        }
    }

    private void handleReadyToClose(ReadyToClose req) {
        synchronized (services) {
            for (var instances : services.values()) {
                instances.removeIf(instance -> instance.getServiceId().equals(req.serviceId));
            }
            services.notifyAll();
        }
    }

    private void handleClosedConnection(ClosedConnection req) {
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
        prepare();

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
