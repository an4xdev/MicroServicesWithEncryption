import Agent.AgentMessage;
import Agent.Requests.ConnectToService;
import Agent.Requests.HelloMessage;
import Agent.Responses.ConnectData;
import Enums.Services;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
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

    public AgentThread(Socket clientSocket, HashMap<Services, ArrayList<ServiceInstance>> services, AtomicInteger port, ArrayDeque<AgentMessage> globalMessages,int agentPort, String agentHost) {
        this.clientSocket = clientSocket;
        this.services = services;
        this.port = port;
        this.globalMessages = globalMessages;
        this.agentPort = agentPort;
        this.agentHost = agentHost;
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
                int servicePort = -1;
                switch (receivedObject) {
                    case HelloMessage message -> {
                        System.out.println("Hello");
                        synchronized (globalMessages) {;
                            globalMessages.add(message);
                            globalMessages.notifyAll();
                        }
                    }
                    case ConnectToService req -> {
                        System.out.println("Connect");
                        synchronized (services) {
                            if (services.containsKey(req.service)) {
                                var instances = services.get(req.service);
                                if (instances != null) {
                                    var instance = instances.getFirst();
                                    if (instance != null) {
                                        var response = new ConnectData(req.messageId,"localhost", instance.getPort());
                                        try {
                                            out.writeObject(response);
                                        } catch (IOException e) {
                                            Utils.logException(e, "Error while sending response.");
                                        }
                                    }
                                }
                            }
                            else {
                                synchronized (port) {
                                    servicePort = port.getAndIncrement();
                                    port.notifyAll();
                                }
                                var serviceInstance = new ServiceInstance(servicePort, req.service, agentPort, agentHost);
                                services.put(req.service, new ArrayList<>() {{
                                    add(serviceInstance);
                                }});
                            }
                            services.notifyAll();
                        }
                        synchronized (globalMessages) {
                            while(true){
                                var message = globalMessages.poll();
                                if(message == null){
                                    continue;
                                }
                                if(message.messageId == req.messageId){
                                    break;
                                }
                                globalMessages.push(message);
                            }
                            globalMessages.notifyAll();
                        }
                        ConnectData response = new ConnectData(req.messageId, "localhost", servicePort);
                        try {
                            out.writeObject(response);
                        } catch (IOException e) {
                            Utils.logException(e, "Error while sending response.");
                        }
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
