import Enums.Services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class ServiceInstance extends Thread {
    private final Services serviceType;
    private final int port;
    private final int agentPort;
    private final String agentHost;
    private final UUID messageId;
    private final UUID serviceId;
    private String serviceName;
    private LocalDateTime lastUsed;

    private boolean isClosing = false;

    public ServiceInstance(
            int port, Services serviceType,
            int agentPort, String agentHost,
            UUID messageId, UUID serviceId) {
        this.port = port;
        this.serviceType = serviceType;
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        this.messageId = messageId;
        this.serviceId = serviceId;
        lastUsed = LocalDateTime.now();
    }

    public int getPort() {
        return port;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        lastUsed = LocalDateTime.now();
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void run() {
        ArrayList<String> arguments = new ArrayList<>() {{
            add("java");
            add("-cp");
            add("D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\Service\\out\\production\\Service\\;D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\Utils\\out\\artifacts\\Utils_jar\\Utils.jar;D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\postgresql-42.7.4.jar");
            add("Main");
        }};

        switch (serviceType) {
            case Register -> serviceName = "Register";
            case Login -> serviceName = "Login";
            case Chat -> serviceName = "Chat";
            case Posts -> serviceName = "Posts";
            case File -> serviceName = "File";
        }

        arguments.add(String.valueOf(serviceType.ordinal()));
        arguments.add(String.valueOf(port));
        arguments.add(String.valueOf(agentPort));
        arguments.add(agentHost);
        arguments.add(messageId.toString());
        arguments.add(serviceId.toString());

        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.inheritIO();
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            Utils.logInfo("Starting service: " + serviceName);
            process = builder.start();
        } catch (IOException e) {
            Utils.logException(e, "Error while starting service: " + serviceName);
//            return;
        }

//        try {
//            process.waitFor();
//        } catch (InterruptedException e) {
//            Utils.logException(e, "Error in running instance of service: " + serviceName);
//        }

    }

    public boolean isClosing() {
        return isClosing;
    }

    public void setClosing(boolean closing) {
        isClosing = closing;
    }
}
