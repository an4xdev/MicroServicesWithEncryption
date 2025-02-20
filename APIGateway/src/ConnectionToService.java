import Agent.Requests.CreatedConnection;
import Messages.BaseForwardRequest;
import Messages.BaseForwardResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.UUID;

public class ConnectionToService implements Runnable {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final int servicePort;
    private final String serviceHost;
    private final UUID apiGatewayId;
    private final UUID targetServiceId;
    private final String targetServiceName;
    private final ArrayDeque<BaseForwardResponse> messagesToReceive;
    private final ConnectionToAgent connectionToAgent;

    private boolean isRunning;

    public ConnectionToService(
            int servicePort, String serviceHost,
            UUID apiGatewayId,
            UUID targetServiceId, String targetServiceName,
            ConnectionToAgent connectionToAgent) {
        this.servicePort = servicePort;
        this.serviceHost = serviceHost;
        this.messagesToReceive = new ArrayDeque<>();
        this.apiGatewayId = apiGatewayId;
        this.targetServiceId = targetServiceId;
        this.targetServiceName = targetServiceName;
        this.connectionToAgent = connectionToAgent;
        prepare();
    }

    private void prepare() {
        int retries = 3;
        while (retries > 0) {
            try {
                socket = new Socket(serviceHost, servicePort);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Utils.logDebug("Successfully connected to service.");
                isRunning = true;
                return;
            } catch (IOException e) {
                retries--;
                Utils.logException(e, "Retrying connection to service... (" + retries + " retries left)");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {

                }
            }
        }
        Utils.logError("Failed to connect to service after retries.");
        cleanResources();
    }

    private void cleanResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    public synchronized <T> T getMessageFromService(UUID messageId, Class<T> clazz) throws InterruptedException {
        while (true) {
            synchronized (messagesToReceive) {
                if (!messagesToReceive.isEmpty()) {
                    BaseForwardResponse message = messagesToReceive.poll();
                    if (message.messageId.equals(messageId) && clazz.isInstance(message)) {
                        messagesToReceive.notify();
                        return clazz.cast(message);
                    } else {
                        messagesToReceive.push(message);
                    }
                }
                messagesToReceive.notify();
            }
        }
    }

    public void addMessage(BaseForwardResponse message) {
        synchronized (messagesToReceive) {
            messagesToReceive.add(message);
            messagesToReceive.notify();
        }
    }

    public void sendMessageToService(BaseForwardRequest request) {
        try {
            out.writeObject(request);
        } catch (Exception e) {
            Utils.logException(e, "Error while sending message to agent.");
            cleanResources();
        }
    }

    public UUID getTargetServiceId() {
        return targetServiceId;
    }

    public void stop() {
        isRunning = false;
        cleanResources();
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        CreatedConnection req = new CreatedConnection(
                UUID.randomUUID(),
                "Api Gateway",
                apiGatewayId,
                targetServiceName,
                targetServiceId
        );

        try {
            connectionToAgent.sendMessageToAgent(req);
        } catch (Exception e) {
            Utils.logException(e, "Error while sending created connection message to agent.");
//            cleanResources();
//            return;
        }

        while (isRunning) {
            Object receivedObject;
            try {
                receivedObject = in.readObject();
                if (receivedObject == null) {
                    break;
                }
                if (receivedObject instanceof BaseForwardResponse res) {
                    addMessage(res);
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

        cleanResources();
    }

    public boolean isRunning() {
        return isRunning;
    }
}
