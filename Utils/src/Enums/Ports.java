package Enums;

public enum Ports {
    ApiGateway(32111),
    Agent(32137);

    private final int port;

    Ports(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
