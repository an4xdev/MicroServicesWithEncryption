import Agent.AgentMessage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.UUID;

public class ConnectionToAgent implements Runnable {

    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int agentPort;
    private final String agentHost;
    private final ArrayDeque<AgentMessage> messagesToReceive;

    public ConnectionToAgent(int agentPort, String agentHost) {
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        this.messagesToReceive = new ArrayDeque<>();
        run();
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

    private final Object lock = new Object();

    public synchronized <T> T receiveMessageFromAgent(UUID messageId, Class<T> clazz) throws InterruptedException {
        synchronized (lock) {
            while (true) {
                if (!messagesToReceive.isEmpty()) {
                    AgentMessage message = messagesToReceive.poll();
                    if (message.messageId.equals(messageId) && clazz.isInstance(message)) {
                        return clazz.cast(message);
                    } else {
                        messagesToReceive.push(message);
                    }
                }
                lock.wait();
            }
        }
    }

    public void addMessage(AgentMessage message) {
        synchronized (lock) {
            messagesToReceive.add(message);
            lock.notify();
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
        prepare();
        while (true) {
            Object receivedObject;
            try {
                receivedObject = in.readObject();
                if (receivedObject == null) {
                    break;
                }
                if (receivedObject instanceof AgentMessage) {
                    addMessage((AgentMessage) receivedObject);
                }
                else {
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
