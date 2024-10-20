import Checking.Ping;
import Checking.Pong;
import Register.RegisterRequest;
import jdk.jshell.execution.Util;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int port = Utils.Ports.ApiGateway.getPort();

        KeyPair keys = null;    
        try {
            keys = Utils.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Utils.logException(e, "Could not generate key pair.");
            System.exit(Utils.Codes.KeyError.ordinal());
        }
        
        PublicKey publicKey = keys.getPublic();
        PrivateKey privateKey = keys.getPrivate();

        SecretKey symetricKey = null;
        try {
            symetricKey = Utils.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            Utils.logException(e, "Could not secret key.");
            System.exit(Utils.Codes.SecretKeyError.ordinal());
        }

        try(Socket socket = new Socket("localhost", port)) {
            Utils.logInfo("Connected to API Gateway on port: " + port);
            var outputStream = new ObjectOutputStream(socket.getOutputStream());
            var inputStream = new ObjectInputStream(socket.getInputStream());

            var APIPublicKey = (PublicKey)inputStream.readObject();
            Utils.logDebug("Got API public key");
            outputStream.writeObject(publicKey);
            outputStream.flush();
            Utils.logDebug("Sent public key to API");
            
            // ping pong data validation
            
            int value = new Random().nextInt();
            var pingOperation = Utils.sendMessage(Ping.class, outputStream, Integer.toString(value), privateKey, APIPublicKey, symetricKey);
            if(!pingOperation.isSuccessful())
            {
                Utils.logError("Could not send ping message: " + pingOperation.message());
                cleanResources(inputStream, outputStream, socket);
                return;
            }
            
            var objResponse = inputStream.readObject();
            if(objResponse instanceof Pong pong)
            {
                var pongOperation = Utils.processMessage(pong.numberValue(), pong.fingerPrint(), pong.encryptedSymmetricKey(), privateKey, APIPublicKey);
                if(!pongOperation.isSuccessful())
                {
                    Utils.logError("Could not process pong message: " + pongOperation.message());
                    cleanResources(inputStream, outputStream, socket);
                    return;
                }
                int pongValue;
                try{
                    pongValue = Integer.parseInt(pongOperation.message());
                } catch (Exception e) {
                    Utils.logError("Could not parse pong value.");
                    cleanResources(inputStream, outputStream, socket);
                    return;
                }
                if(pongValue - 10 == value)
                {
                    Utils.logDebug("Ping pong data validation successful.");
                }
                else
                {
                    Utils.logError("Ping pong data validation failed.");
                    cleanResources(inputStream, outputStream, socket);
                    return;
                }
            }
            else
            {
                Utils.logError("Invalid response from API Gateway.");
                cleanResources(inputStream, outputStream, socket);
                return;
            }
            
            var in = new BufferedReader(new InputStreamReader(System.in));
            int option = 0;
            while (option != 6) {
                printMenu();
                System.out.println("Choose option: ");
                try{
                    option = Integer.parseInt(in.readLine());
                } catch (Exception e) {
                    Utils.logException(e, "Could not read option.");
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
                        System.out.println("Enter your username: ");
                        String data = in.readLine();
                        var registerOperation = Utils.sendMessage(RegisterRequest.class, outputStream, data, privateKey, APIPublicKey, symetricKey);
                        if(!registerOperation.isSuccessful())
                        {
                            Utils.logError("Could not send registration message: " + registerOperation.message());
                            break;
                        }
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
            cleanResources(inputStream, outputStream, socket);
        } catch (IOException e) {
            Utils.logException(e, "Could not connect to API Gateway on port: " + port + ".");
        }
        catch (ClassNotFoundException e) {
            Utils.logException(e, "Could not read object from input stream.");
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
    
    private static void cleanResources(ObjectInputStream in, ObjectOutputStream out, Socket socket)
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
        } catch (IOException e) {
            Utils.logException(e, "Could not clean resources.");
        }
    }
}