package Agent.Requests;

import Agent.AgentMessage;

public class ConnectToService extends AgentMessage {
    public String serviceName;

    public ConnectToService(String serviceName) {
        this.serviceName = serviceName;
    }
}
