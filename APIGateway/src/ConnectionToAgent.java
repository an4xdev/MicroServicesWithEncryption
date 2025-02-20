import Agent.AgentMessage;
import Agent.Requests.CloseConnection;
import Agent.Responses.ClosedConnection;
import Enums.Services;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ConnectionToAgent implements Runnable {

    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int agentPort;
    private final String agentHost;
    private final ArrayDeque<AgentMessage> messagesToReceive;
    private final HashMap<Services, ArrayList<ConnectionToService>> connections;
    private final UUID apiGatewayId;

    public ConnectionToAgent(
            int agentPort, String agentHost,
            HashMap<Services, ArrayList<ConnectionToService>> connections, UUID apiGatewayId
    ) {
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        this.connections = connections;
        this.apiGatewayId = apiGatewayId;
        this.messagesToReceive = new ArrayDeque<>();
        prepare();
    }

    private void prepare() {
        try {
            clientSocket = new Socket(agentHost, agentPort);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
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

    private final Object lockReceive = new Object();

    public synchronized <T> T receiveMessageFromAgent(UUID messageId, Class<T> clazz) throws InterruptedException {
        while (true) {
            synchronized (messagesToReceive) {
                if (!messagesToReceive.isEmpty()) {
                    AgentMessage message = messagesToReceive.poll();
                    if (message.messageId.equals(messageId) && clazz.isInstance(message)) {
                        messagesToReceive.notify();
                        return clazz.cast(message);
                    } else {
                        messagesToReceive.push(message);
                    }
                }
                messagesToReceive.notify();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Utils.logException(e, "Error while sleeping.");
            }
        }
    }

    private final Object lockAdd = new Object();

    public void addMessage(AgentMessage message) {
        synchronized (lockAdd) {
            messagesToReceive.add(message);
            lockAdd.notify();
        }
    }

    public synchronized void sendMessageToAgent(AgentMessage message) {
        try {
            out.writeObject(message);
        } catch (Exception e) {
            Utils.logException(e, "Error while sending message to agent.");
            cleanResources();
        }
    }

    @Override
    public void run() {
        while (true) {
            Object receivedObject;
            try {
                receivedObject = in.readObject();
                if (receivedObject == null) {
                    break;
                }
                if (receivedObject instanceof AgentMessage) {
                    if (receivedObject instanceof CloseConnection close) {
                        synchronized (connections) {
                            var listCollection = connections.values();
                            for (var connection : listCollection) {
                                for (var toService : connection) {
                                    if (toService.getTargetServiceId().equals(close.targetServiceId)) {
                                        toService.stop();
                                        
                                        connection.remove(toService);

                                        sendMessageToAgent(new ClosedConnection(close.messageId, apiGatewayId, close.targetServiceId));

                                        break;
                                    }
                                }
                            }
                            connections.notifyAll();
                        }
                    } else {
                        addMessage((AgentMessage) receivedObject);
                    }
                } else {
                    Utils.logError("Unknown message.");
                    cleanResources();
                    return;
                }
            } catch (Exception e) {
                Utils.logException(e, "Error while receiving message from agent.");
                cleanResources();
                return;
            }
        }
    }
}
