import Agent.Requests.Close;
import Agent.Requests.CloseConnection;
import Enums.Services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class CheckServicesActivityThread implements Runnable {

    private final ArrayList<ConnectionBetweenServices> connections;
    private final HashMap<Services, ArrayList<ServiceInstance>> services;
    private final ArrayList<AgentThread> agentThreads;
    private final int CLOSE_CONNECTION_AFTER_SECONDS = 30;
    private final int CLOSE_SERVICE_AFTER_SECONDS = 60;
    private final int CHECK_EVERY_SECONDS = 5 * 1000;

    public CheckServicesActivityThread(
            ArrayList<ConnectionBetweenServices> connections,
            HashMap<Services, ArrayList<ServiceInstance>> services,
            ArrayList<AgentThread> agentThreads
    ) {
        this.connections = connections;
        this.services = services;
        this.agentThreads = agentThreads;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (connections) {
                for (ConnectionBetweenServices connection : connections) {
                    if (
                            Duration.between(
                                    connection.getLastActivity().plusSeconds(CLOSE_CONNECTION_AFTER_SECONDS),
                                    LocalDateTime.now()
                            ).isPositive() && !connection.isClosing()
                    ) {
                        synchronized (agentThreads) {
                            for (var agentThread : agentThreads) {
                                if (agentThread.getServiceId() == connection.getSourceServiceId()) {

                                    CloseConnection closeConnection = new CloseConnection(
                                            UUID.randomUUID(),
                                            connection.getTargetServiceId()
                                    );
                                    agentThread.sendMessage(closeConnection);
                                    break;
                                }
                            }
                            agentThreads.notifyAll();
                        }

                        connection.setClosing(true);
                    }
                }
                connections.notifyAll();
            }

            synchronized (services) {
                for (Services service : services.keySet()) {
                    for (ServiceInstance serviceInstance : services.get(service)) {
                        if (
                                Duration.between(
                                        serviceInstance.getLastUsed().plusSeconds(CLOSE_SERVICE_AFTER_SECONDS),
                                        LocalDateTime.now()
                                ).isPositive() && !serviceInstance.isClosing()
                        ) {
                            Close close = new Close(UUID.randomUUID());
                            synchronized (agentThreads) {
                                for (var agentThread : agentThreads) {
                                    if (agentThread.getServiceId() == serviceInstance.getServiceId()) {
                                        agentThread.sendMessage(close);
                                        break;
                                    }
                                }
                                agentThreads.notifyAll();
                            }
                            serviceInstance.setClosing(true);
                        }
                    }
                }
                services.notifyAll();
            }

            try {
                Thread.sleep(CHECK_EVERY_SECONDS);
            } catch (InterruptedException e) {
                Utils.logException(e, "Error in CheckServicesActivityThread on Thread.sleep().");
            }
        }
    }
}
