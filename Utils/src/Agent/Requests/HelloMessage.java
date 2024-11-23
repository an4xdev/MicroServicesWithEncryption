package Agent.Requests;

import Agent.AgentMessage;

public class HelloMessage extends AgentMessage {
    public String serviceName;

    public HelloMessage(String serviceName) {
        this.serviceName = serviceName;
    }
}
