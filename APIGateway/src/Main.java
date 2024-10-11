import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = Utils.Ports.ApiGateway.getPort();
        if(args.length != 1)
        {
            System.err.println("Usage: java Main [debug]");
            System.exit(1);
        }
        boolean debug = args[0].equals("debug");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Api Gateway started on port " + port);
            while (true) {
                try(Socket socket = serverSocket.accept()) {
                    new ApiGatewayThread(debug, socket);
                } catch (IOException e) {
                    Utils.logError(debug, e, "Error while creating socket from incoming connection");
                }
            }
        } catch (IOException e) {
            Utils.logError(true, e, "Error while creating server socket");
        }
    }
}