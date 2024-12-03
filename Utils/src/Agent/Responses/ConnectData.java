package Agent.Responses;

import Agent.AgentMessage;

import java.util.UUID;

public class ConnectData extends AgentMessage {
    public String host;
    public int port;

    public ConnectData(UUID messageId, String host, int port) {
        this.messageId = messageId;
        this.host = host;
        this.port = port;
    }
}
