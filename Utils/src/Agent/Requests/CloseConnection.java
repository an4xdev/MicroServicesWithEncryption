package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class CloseConnection extends AgentMessage {
    public UUID targetServiceId;

    public CloseConnection(UUID messageId, UUID targetServiceId) {
        this.messageId = messageId;
        this.targetServiceId = targetServiceId;
    }
}
