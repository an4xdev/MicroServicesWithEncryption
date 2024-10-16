import Register.RegisterRequest;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Main {
    private static boolean debug = false;
    public static void main(String[] args) {
        debug = Utils.debug;
        int port = Utils.Ports.ApiGateway.getPort();

        KeyPair keys = null;    
        try {
            keys = Utils.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Utils.logError(debug, e, "Could not generate key pair.");
            System.exit(Utils.Codes.KeyError.ordinal());
        }
        
        PublicKey publicKey = keys.getPublic();
        PrivateKey privateKey = keys.getPrivate();

        SecretKey symetricKey = null;
        try {
            symetricKey = Utils.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            Utils.logError(debug, e, "Could not secret key.");
            System.exit(Utils.Codes.SecretKeyError.ordinal());
        }

        try(Socket socket = new Socket("localhost", port)) {
            if(debug)
            {
                System.out.println("Connected to API Gateway on port: " + port);
            }
            var outputStream = new ObjectOutputStream(socket.getOutputStream());
            var inputStream = new ObjectInputStream(socket.getInputStream());

            var APIPublicKey = (PublicKey)inputStream.readObject();
            if(debug)
            {
                System.out.println("Got API public key");
            }
            outputStream.writeObject(publicKey);
            outputStream.flush();
            if(debug)
            {
                System.out.println("Sent public key to API");
            }
            var in = new BufferedReader(new InputStreamReader(System.in));
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
                        System.out.println("Enter your username: ");
                        String data = in.readLine();

                        // Haszowanie danych
                        byte[] hashedData = Utils.hashData(data);
                        
                        // Podpisanie kluczem prywatnym
                        byte[] singedHash = Utils.signData(hashedData, privateKey);
                        
                        // Szyfrowanie danych kluczem symetrycznym 
                        byte[] dataWithSymetricKey = Utils.encrypt(data.getBytes(StandardCharsets.UTF_8), symetricKey);

                        // Szyfrowanie podpisu kluczem symetrycznym
                        byte[] fingerprintWithSymetricKey = Utils.encrypt(singedHash, symetricKey);
                        
                        // Szyfrowanie klucza symetrycznego kluczem publicznym odbiorcy
                        byte[] encryptedSecretKey = Utils.encryptKey(symetricKey, APIPublicKey);
                        
                        var registerRequest = new RegisterRequest(dataWithSymetricKey,fingerprintWithSymetricKey, encryptedSecretKey);
                        outputStream.writeObject(registerRequest);
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
    
    private static void cleanResources(InputStream in, OutputStream out, Socket socket)
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
    
    private static String processRegister()
    {
        
        return "";
    }
}