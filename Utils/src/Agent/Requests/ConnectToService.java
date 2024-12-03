package Agent.Requests;

import Agent.AgentMessage;
import Enums.Services;

import java.util.UUID;

public class ConnectToService extends AgentMessage {
    public Services service;

    public ConnectToService(UUID messageId, Services service) {
        this.messageId = messageId;
        this.service = service;
    }
}
