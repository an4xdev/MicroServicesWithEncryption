package Agent.Responses;

import Agent.AgentMessage;

import java.util.UUID;

public class ReadyToClose extends AgentMessage {

    public UUID serviceId;

    public ReadyToClose(UUID messageId, UUID serviceId) {
        this.messageId = messageId;
        this.serviceId = serviceId;
    }
}
