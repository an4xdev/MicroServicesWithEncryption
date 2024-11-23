import Agent.AgentMessage;
import Agent.Requests.HelloMessage;
import Agent.Responses.ConnectData;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AgentThread implements Runnable {

    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public AgentThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
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
                    case HelloMessage _ -> {
                        System.out.println("Hello");
                    }
                    case ConnectData _ -> {
                        System.out.println("Connect");
                    }
                    default -> Utils.logError("Unknown message type.");
                }
            } else {
                Utils.logError("Unknown data.");
            }
        }

        cleanResources();
    }
}
