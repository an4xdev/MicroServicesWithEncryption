package Agent.Responses;

import Agent.AgentMessage;

import java.util.UUID;

public class ClosedConnection extends AgentMessage {
    public UUID sourceServiceId;
    public UUID targetServiceId;

    public ClosedConnection(UUID messageId, UUID sourceServiceId, UUID targetServiceId) {
        this.messageId = messageId;
        this.sourceServiceId = sourceServiceId;
        this.targetServiceId = targetServiceId;
    }
}
