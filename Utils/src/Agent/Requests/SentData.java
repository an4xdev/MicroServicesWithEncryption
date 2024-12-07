package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class SentData extends AgentMessage {
    public UUID serviceId;

    public SentData(UUID messageId, UUID serviceId) {
        this.messageId = messageId;
        this.serviceId = serviceId;
    }
}
