package Agent.Requests;

import Agent.AgentMessage;

import java.util.UUID;

public class Close extends AgentMessage {
    
    public Close(UUID messageId) {
        this.messageId = messageId;
    }
}
