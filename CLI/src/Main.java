import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Main {
    private static boolean debug = false;
    public static void main(String[] args) {
        if(args.length != 1)
        {
            System.err.println("Usage: java Main [debug]");
            System.exit(Utils.Codes.NoParameters.ordinal());
        }
        debug = args[0].equals("debug");
        int port = Utils.Ports.ApiGateway.getPort();

        KeyPair keys = null;
        try {
            keys = Utils.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Utils.logError(debug, e, "Could not generate key pair.");
            System.exit(Utils.Codes.KeyGenerationError.ordinal());
        }

        System.out.println("Key public getAlgorithm: " + keys.getPublic().getAlgorithm());
        System.out.println("Key public getEncoded: " + Arrays.toString(keys.getPublic().getEncoded()));
        System.out.println("Key public getFormat: " + keys.getPublic().getFormat());

        System.out.println("-----------------------------");
        
        System.out.println("Key private getAlgorithm: " + keys.getPrivate().getAlgorithm());
        System.out.println("Key private getEncoded: " + Arrays.toString(keys.getPrivate().getEncoded()));
        System.out.println("Key private getFormat: " + keys.getPrivate().getFormat());
        

        System.exit(0);
        
        try(Socket socket = new Socket("localhost", port)) {
            
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            int option = 0;
            while (option != 6) {
                printMenu();
                System.out.println("Choose option: ");
                try{
                    option = Integer.parseInt(in.readLine());
                } catch (Exception e) {
                    Utils.logError(debug, e, "Could not read option.");
                    continue;
                }
                if(option < 1 || option > 6)
                {
                    System.out.println("Invalid option");
                    continue;
                }
                switch (option)
                {
                    case 1 -> {
                        System.out.println("Register");
                    }
                    case 2 -> {
                        System.out.println("Login");
                    }
                    case 3 -> {
                        System.out.println("Send post");
                    }
                    case 4 -> {
                        System.out.println("See 10 last post");
                    }
                    case 5 -> {
                        System.out.println("Send file");
                    }
                    case 6 -> {
                        System.out.println("Exit");
                    }
                    
                    default -> throw new IllegalStateException("Unexpected value: " + option);
                }
            }
            cleanResources(null, null, socket);
        } catch (Exception e) {
            Utils.logError(debug, e, "Could not connect to API Gateway on port: " + port + ".");
        }
    }
    
    private static void printMenu()
    {
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Send post");
        System.out.println("4. See 10 last post");
        System.out.println("5. Send file");
        System.out.println("6. Exit");
    }
    
    private static void cleanResources(InputStream in, Writer out, Socket socket)
    {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logError(debug, e, "Could not clean resources.");
        }
    }
}