import Agent.Requests.HelloMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ServiceToAgentThread implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int agentPort;
    private final String agentHost;
    private final UUID firstMessageId;
    private final String serviceName;

    public ServiceToAgentThread(int agentPort, String agentHost, UUID firstMessageId, String serviceName) {
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        this.firstMessageId = firstMessageId;
        this.serviceName = serviceName;
        run();
    }

    private void prepare() {
        try {
            socket = new Socket(agentHost, agentPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
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
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    @Override
    public void run() {
        prepare();
        HelloMessage helloMessage = new HelloMessage(firstMessageId, serviceName);
        try {
            out.writeObject(helloMessage);
        } catch (IOException e) {
            Utils.logException(e, "Error while sending hello message.");
            cleanResources();
            return;
        }

        while (true) {
            Object receivedObject;
            try {
                receivedObject = in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Utils.logException(e, "Error while reading object from input stream.");
                cleanResources();
                return;
            }

        }
    }
}
