import Messages.BaseForwardRequest;
import Messages.BaseForwardResponse;

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
    private final ArrayDeque<BaseForwardResponse> messagesToReceive;

    public ConnectionToService(int servicePort, String serviceHost) {
        this.servicePort = servicePort;
        this.serviceHost = serviceHost;
        this.messagesToReceive = new ArrayDeque<>();
        run();
    }

    private void prepare() {
        try {
            socket = new Socket(serviceHost, servicePort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
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
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    private final Object lock = new Object();

    public synchronized <T> T getMessageFromService(UUID messageId, Class<T> clazz) throws InterruptedException {
        synchronized (lock) {
            while (true) {
                if (!messagesToReceive.isEmpty()) {
                    BaseForwardResponse message = messagesToReceive.poll();
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

    public void addMessage(BaseForwardResponse message) {
        synchronized (lock) {
            messagesToReceive.add(message);
            lock.notify();
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

    /**
     * Runs this operation.
     */
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
                if (receivedObject instanceof BaseForwardResponse res) {
                    addMessage(res);
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
