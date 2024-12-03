import Enums.Services;

import java.io.IOException;
import java.util.ArrayList;

public class ServiceInstance extends Thread{
    private final Services serviceType;
    private final int port;
    private final int agentPort;
    private final String agentHost;
    private String serviceName;

    public ServiceInstance(int port, Services serviceType, int agentPort, String agentHost) {
        this.port = port;
        this.serviceType = serviceType;
        this.agentPort = agentPort;
        this.agentHost = agentHost;
        start();
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        ArrayList<String> arguments = new ArrayList<>(){{
            add("java");
            add("-cp");
            add("D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\Service\\out\\production\\Service\\;D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\Utils\\out\\artifacts\\Utils_jar\\Utils.jar");
            add("Main");
        }};

        switch(serviceType){
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

        ProcessBuilder builder = new ProcessBuilder(arguments).inheritIO().redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            Utils.logException(e, "Error while starting service: " + serviceType);
            return;
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Utils.logException(e, "Error in running instance of service: " + serviceType);
        }

    }
}
