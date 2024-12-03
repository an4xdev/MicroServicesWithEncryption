package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class HelloMessage extends AgentMessage {
    public String serviceName;

    public HelloMessage(UUID messageId, String serviceName) {
        this.messageId = messageId;
        this.serviceName = serviceName;
    }
}
