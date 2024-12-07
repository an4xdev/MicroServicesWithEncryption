package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class RegisterToAgent extends AgentMessage {
    public UUID serviceId;
    public String serviceName;

    public RegisterToAgent(UUID messageId, UUID serviceId, String serviceName) {
        this.messageId = messageId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }
}
