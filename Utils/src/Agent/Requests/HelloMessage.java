package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class HelloMessage extends AgentMessage {
    public String serviceName;
    public UUID serviceId;

    public HelloMessage(UUID messageId, String serviceName, UUID serviceId) {
        this.messageId = messageId;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
    }
}
