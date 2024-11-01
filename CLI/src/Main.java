import Register.RegisterForwardResponse;
import Register.RegisterRequest;
import Register.RegisterResponse;
import jdk.jshell.execution.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int port = Utils.Ports.ApiGateway.getPort();
        int user_id = -1;

        KeyPair keys = null;    
        try {
            keys = Utils.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Utils.logException(e, "Could not generate key pair.");
            System.exit(Utils.Codes.KeyError.ordinal());
        }
        
        PublicKey publicKey = keys.getPublic();
        PrivateKey privateKey = keys.getPrivate();

        SecretKey symmetricKey = null;
        try {
            symmetricKey = Utils.generateSecretKey();
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

            Utils.logDebug("Starting ping pong data validation.");

            Utils.logDebug("Sending ping request to API.");
            
            int value = new Random().nextInt();

            byte[] pingRequest = Utils.encryptPing(value, APIPublicKey);

            outputStream.writeObject(pingRequest);

            // get response

            int pongValue = inputStream.readInt();

            Utils.logDebug("Got pong response from API.");

            if(pongValue - 10 == value)
            {
                Utils.logDebug("Ping pong data validation successful.");
            }
            else
            {
                Utils.logDebug("Ping pong data validation failed.");
                Utils.logError("Ping pong data validation failed.");
                cleanResources(inputStream, outputStream, socket);
                return;
            }

            // get data to validate from API

            byte[] pingRequestAPI = (byte[])inputStream.readObject();

            Utils.logDebug("Got data to validate from API.");

            int pongValueAPI = Utils.decryptPing(pingRequestAPI, privateKey);

            pongValueAPI += 10;

            Utils.logDebug("Sending pong response to API.");

            outputStream.writeInt(pongValueAPI);
            outputStream.flush();

            int code = inputStream.readInt();

            if(code != 200)
            {
                Utils.logError("Connection verification gone wrong.");
                cleanResources(inputStream, outputStream, socket);
                return;
            }

            byte[] key;
            try {
                key = Utils.encryptKey(symmetricKey, APIPublicKey);
            } catch (Exception e) {
                Utils.logException(e, "Could not encrypt symmetric key.");
                cleanResources(inputStream, outputStream, socket);
                return;
            }

            outputStream.writeObject(new SymmetricKeyMessage(key));

            Utils.logDebug("Sent symmetric key to API.");

            Utils.logInfo("Connection is successfully established with API Gateway.");

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
                        var registerOperation = Utils.sendMessage(RegisterRequest.class, outputStream, data, privateKey, symmetricKey);
                        if(!registerOperation.isSuccessful())
                        {
                            Utils.logError("Could not send registration message: " + registerOperation.message());
                            break;
                        }

                        RegisterResponse response = (RegisterResponse) inputStream.readObject();
                        var registerResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);
                        if(!registerResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse registration response: " + registerResponseOperation.message());
                            break;
                        }
                        RegisterForwardResponse obj = RegisterForwardResponse.ConvertFromString(registerResponseOperation.message());
                        if(obj.code > 300){
                            Utils.logError("Registration failed: "+ obj.message);
                            break;
                        }
                        Utils.logInfo("Registration completed successfully.");
                        user_id = obj.userId;
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
            Utils.logException(e, "Input output operations failed.");
        }
        catch (ClassNotFoundException e) {
            Utils.logException(e, "Could not recognize object from input stream.");
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException e) {
            Utils.logException(e, "Could not encrypt validation data.");
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