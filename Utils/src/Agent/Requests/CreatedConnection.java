package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class CreatedConnection extends AgentMessage {
    public String sourceService;
    public UUID sourceServiceId;
    public String targetService;
    public UUID targetServiceId;

    public CreatedConnection(UUID messageId, String sourceService, UUID sourceServiceId, String targetService, UUID targetServiceId) {
        this.messageId = messageId;
        this.sourceService = sourceService;
        this.sourceServiceId = sourceServiceId;
        this.targetService = targetService;
        this.targetServiceId = targetServiceId;
    }
}
