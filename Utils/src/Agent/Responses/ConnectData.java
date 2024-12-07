package Agent.Responses;

import Agent.AgentMessage;

import java.util.UUID;

public class ConnectData extends AgentMessage {
    public String host;
    public int port;
    public UUID serviceId;
    public String serviceName;

    public ConnectData(UUID messageId, String host, int port, UUID serviceId, String serviceName) {
        this.messageId = messageId;
        this.host = host;
        this.port = port;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }
}
