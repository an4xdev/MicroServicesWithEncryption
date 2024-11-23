package Agent.Responses;

import Agent.AgentMessage;

public class ConnectData extends AgentMessage {
    public String host;
    public int port;

    public ConnectData(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
