import Enums.Ports;
import Files.DownloadFile.DownloadFileForwardRequest;
import Files.DownloadFile.DownloadFileForwardResponse;
import Files.DownloadFile.DownloadFileRequest;
import Files.DownloadFile.DownloadFileResponse;
import Files.GetExisting.GetExistingForwardRequest;
import Files.GetExisting.GetExistingForwardResponse;
import Files.GetExisting.GetExistingRequest;
import Files.GetExisting.GetExistingResponse;
import Files.Send.SendFileForwardRequest;
import Files.Send.SendFileForwardResponse;
import Files.Send.SendFileRequest;
import Files.Send.SendFileResponse;
import Login.LoginForwardRequest;
import Login.LoginForwardResponse;
import Login.LoginRequest;
import Login.LoginResponse;
import Post.Add.AddPostForwardRequest;
import Post.Add.AddPostForwardResponse;
import Post.Add.AddPostRequest;
import Post.Add.AddPostResponse;
import Post.Get.GetPostsForwardRequest;
import Post.Get.GetPostsForwardResponse;
import Post.Get.GetPostsRequest;
import Post.Get.GetPostsResponse;
import Register.RegisterForwardRequest;
import Register.RegisterForwardResponse;
import Register.RegisterRequest;
import Register.RegisterResponse;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class Main {

    private static final String FILE_PATH = "keys.txt";
    public static PublicKey APIPublicKey;

    public static void main(String[] args) {
        int port = Ports.ApiGateway.getPort();
        int userId = -1;
        boolean logged = false;

        KeyPair keys = null;
        if (Files.exists(Paths.get(FILE_PATH))) {
            keys = readKeyPairFromFile();
            if (keys == null) {
                return;
            }
        } else {
            try {
                keys = Utils.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                Utils.logException(e, "Could not generate key pair.");
                System.exit(Utils.Codes.KeyError.ordinal());
                return;
            }
            if (keys != null) {
                if(!saveKeyPairToFile(keys)) {
                    return;
                }
            }
        }

        assert keys != null;
        PublicKey publicKey = keys.getPublic();
        PrivateKey privateKey = keys.getPrivate();

        SecretKey symmetricKey = null;
        try {
            symmetricKey = Utils.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            Utils.logException(e, "Could not secret key.");
            System.exit(Utils.Codes.SecretKeyError.ordinal());
        }

        try (Socket socket = new Socket("localhost", port)) {
            Utils.logDebug("Connected to API Gateway on port: " + port);
            Utils.logInfo("Connected to API Gateway, starting verification.");
            var outputStream = new ObjectOutputStream(socket.getOutputStream());
            var inputStream = new ObjectInputStream(socket.getInputStream());

            try {
                if(!connectToAPI(socket, inputStream, outputStream, publicKey, privateKey, symmetricKey)) {
                    return;
                }
            } catch (Exception e) {
                return;
            }

            var in = new BufferedReader(new InputStreamReader(System.in));
            int option = 0;
            while (option != 8) {
                UUID messageID = UUID.randomUUID();
                printMenu();
                System.out.println("Choose option: ");
                try {
                    option = Integer.parseInt(in.readLine());
                } catch (Exception e) {
                    Utils.logException(e, "Could not read option.");
                    continue;
                }
                if (option < 1 || option > 8) {
                    Utils.logError("Invalid option");
                    continue;
                }
                switch (option) {
                    case 1 -> {
                        System.out.println("Enter your username: ");
                        String data = in.readLine();
                        RegisterForwardRequest request = new RegisterForwardRequest();
                        request.login = data;
                        request.messageId = messageID;
                        request.publicKey = publicKey;
                        var registerOperation = Utils.sendMessage(RegisterRequest.class, outputStream, RegisterForwardRequest.ConvertToString(request), privateKey, symmetricKey);
                        if (!registerOperation.isSuccessful()) {
                            Utils.logError("Could not send registration message: " + registerOperation.message());
                            break;
                        }

                        RegisterResponse response = (RegisterResponse) inputStream.readObject();
                        var registerResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);
                        if (!registerResponseOperation.isSuccessful()) {
                            Utils.logError("Could not parse registration response: " + registerResponseOperation.message());
                            break;
                        }
                        RegisterForwardResponse obj = RegisterForwardResponse.ConvertFromString(registerResponseOperation.message());
                        if (obj.code > 300) {
                            Utils.logError("Registration failed: " + obj.message);
                            break;
                        }
                        Utils.logInfo("Registration completed successfully.");
                    }
                    case 2 -> {
                        System.out.println("Enter your username: ");
                        String data = in.readLine();

                        LoginForwardRequest request = new LoginForwardRequest();
                        request.login = data;
                        request.messageId = messageID;
                        request.publicKey = publicKey;

                        var loginOperation = Utils.sendMessage(LoginRequest.class, outputStream, LoginForwardRequest.ConvertToString(request), privateKey, symmetricKey);
                        if(!loginOperation.isSuccessful()){
                            Utils.logError("Could not send registration message: " + loginOperation.message());
                            break;
                        }

                        LoginResponse response = (LoginResponse) inputStream.readObject();
                        var loginResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);

                        if(!loginResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse login response: " + loginResponseOperation.message());
                            break;
                        }

                        LoginForwardResponse obj = LoginForwardResponse.ConvertFromString(loginResponseOperation.message());
                        if(obj.code > 300){
                            Utils.logError("Login failed: " + obj.message);
                            break;
                        }

                        Utils.logInfo("Successfully logged to system.");
                        userId = obj.userId;
                        logged = true;
                    }
                    case 3 -> {
                        if (!logged) {
                            Utils.logError("You must login first.");
                            break;
                        }

                        AddPostForwardRequest request = new AddPostForwardRequest();
                        System.out.println("Write your post:");
                        request.messageId = messageID;
                        request.post = in.readLine();
                        request.userId = userId;

                        var addPostOperation = Utils.sendMessage(AddPostRequest.class, outputStream, AddPostForwardRequest.ConvertToString(request), privateKey, symmetricKey);
                        if(!addPostOperation.isSuccessful()){
                            Utils.logError("Could not send adding new post message: " + addPostOperation.message());
                            break;
                        }

                        AddPostResponse response = (AddPostResponse) inputStream.readObject();
                        var addPostResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);

                        if(!addPostResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse adding post response: " + addPostResponseOperation.message());
                            break;
                        }

                        AddPostForwardResponse obj = AddPostForwardResponse.ConvertFromString(addPostResponseOperation.message());
                        if(obj.code > 300){
                            Utils.logError("Adding post failed: " + obj.message);
                            break;
                        }

                        Utils.logInfo("Successfully added new post.");

                    }
                    case 4 -> {
                        if (!logged) {
                            Utils.logError("You must login first.");
                            break;
                        }

                        GetPostsForwardRequest request = new GetPostsForwardRequest();
                        request.messageId = messageID;

                        var getPostsOperation = Utils.sendMessage(GetPostsRequest.class, outputStream, GetPostsForwardRequest.ConvertToString(request), privateKey, symmetricKey);
                        if(!getPostsOperation.isSuccessful()){
                            Utils.logError("Could not send getting posts message: " + getPostsOperation.message());
                            break;
                        }

                        GetPostsResponse response = (GetPostsResponse) inputStream.readObject();

                        var getPostsResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);

                        if(!getPostsResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse get posts response: " + getPostsResponseOperation.message());
                            break;
                        }

                        GetPostsForwardResponse obj = GetPostsForwardResponse.ConvertFromString(getPostsResponseOperation.message());

                        if(obj.code > 300){
                            Utils.logError("Getting posts failed: " + obj.message);
                            break;
                        }

                        System.out.println("Posts:");

                        for (var post : obj.posts) {
                            System.out.printf("\t%s: %s\n", post.usesLogin, post.message);
                        }
                    }
                    case 5 -> {
                        if (!logged) {
                            Utils.logError("You must login first.");
                            break;
                        }
                        System.out.println("Enter file path: ");
                        String filePath = in.readLine();
                        File file = new File(filePath);
                        if (!file.exists()) {
                            Utils.logError("File does not exist.");
                            break;
                        }
                        SendFileForwardRequest request = new SendFileForwardRequest();
                        request.messageId = messageID;
                        request.fileName = file.getName();
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        request.fileData = Base64.getEncoder().encodeToString(fileBytes);
                        var fileSendOperation = Utils.sendMessage(SendFileRequest.class, outputStream, SendFileForwardRequest.ConvertToString(request), privateKey, symmetricKey);

                        if(!fileSendOperation.isSuccessful()){
                            Utils.logError("Could not upload file: " + fileSendOperation.message());
                            break;
                        }

                        SendFileResponse response = (SendFileResponse) inputStream.readObject();
                        var fileSendResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);
                        if(!fileSendResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse upload file response: " + fileSendResponseOperation.message());
                            break;
                        }

                        SendFileForwardResponse obj = SendFileForwardResponse.ConvertFromString(fileSendResponseOperation.message());

                        if(obj.code > 300){
                            Utils.logError("Getting posts failed: " + obj.message);
                            break;
                        }

                        Utils.logInfo("File uploaded successfully.");
                    }
                    case 6 -> {
                        if (!logged) {
                            Utils.logError("You must login first.");
                            break;
                        }

                        Utils.logInfo("Getting existing files.");

                        GetExistingForwardRequest request = new GetExistingForwardRequest();
                        request.messageId = messageID;

                        var getExistingOperation = Utils.sendMessage(GetExistingRequest.class, outputStream, GetExistingForwardRequest.ConvertToString(request), privateKey, symmetricKey);

                        if(!getExistingOperation.isSuccessful()){
                            Utils.logError("Could not get existing files: " + getExistingOperation.message());
                            break;
                        }

                        GetExistingResponse response = (GetExistingResponse) inputStream.readObject();

                        var getExistingResponseOperation = Utils.processMessage(response.data, response.fingerPrint, APIPublicKey, symmetricKey);

                        if(!getExistingResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse get existing files response: " + getExistingResponseOperation.message());
                            break;
                        }

                        GetExistingForwardResponse obj = GetExistingForwardResponse.ConvertFromString(getExistingResponseOperation.message());

                        if(obj.code > 300){
                            Utils.logError("Getting existing files failed: " + obj.message);
                            break;
                        }

                        if(obj.fileNames.isEmpty()){
                            Utils.logInfo("No files to download.");
                            break;
                        }

                        System.out.println("Files:");
                        for (var fileName : obj.fileNames) {
                            System.out.println("\t" + fileName);
                        }

                        String fileName = in.readLine();

                        while (!obj.fileNames.contains(fileName)) {
                            Utils.logError("File does not exist. Choose from existing files.");
                            fileName = in.readLine();
                        }

                        Utils.logInfo("Downloading file: " + fileName);

                        DownloadFileForwardRequest downloadRequest = new DownloadFileForwardRequest();
                        downloadRequest.messageId = messageID;
                        downloadRequest.fileName = fileName;

                        var downloadOperation = Utils.sendMessage(DownloadFileRequest.class, outputStream, DownloadFileForwardRequest.ConvertToString(downloadRequest), privateKey, symmetricKey);

                        if(!downloadOperation.isSuccessful()){
                            Utils.logError("Could not download file: " + downloadOperation.message());
                            break;
                        }

                        DownloadFileResponse downloadResponse = (DownloadFileResponse) inputStream.readObject();

                        var downloadResponseOperation = Utils.processMessage(downloadResponse.data, downloadResponse.fingerPrint, APIPublicKey, symmetricKey);

                        if(!downloadResponseOperation.isSuccessful()){
                            Utils.logError("Could not parse download file response: " + downloadResponseOperation.message());
                            break;
                        }

                        DownloadFileForwardResponse downloadObj = DownloadFileForwardResponse.ConvertFromString(downloadResponseOperation.message());

                        if(downloadObj.code > 300){
                            Utils.logError("Downloading file failed: " + downloadObj.message);
                            break;
                        }

                        byte[] fileBytes = Base64.getDecoder().decode(downloadObj.fileData);
                        try (FileOutputStream fos = new FileOutputStream(downloadObj.fileName)) {
                            fos.write(fileBytes);
                        }
                        catch (IOException e) {
                            Utils.logException(e, "Could not save file.");
                            break;
                        }

                        Utils.logInfo("File downloaded successfully.");

                    }
                    case 7 -> {
                        Utils.logInfo("Logout.");
                        logged = false;
                    }
                    case 8 -> {
                        Utils.logInfo("Exiting program.");
                    }

                    default -> throw new IllegalStateException("Unexpected value: " + option);
                }
            }
            cleanResources(inputStream, outputStream, socket);
        } catch (IOException e) {
            Utils.logException(e, "Input output operations failed.");
        } catch (ClassNotFoundException e) {
            Utils.logException(e, "Could not recognize object from input stream.");
        }
    }

    private static boolean saveKeyPairToFile(KeyPair keyPair) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(keyPair);
        } catch (IOException e) {
            Utils.logException(e, "Some error happened in application initialization.");
            return false;
        }
        return true;
    }

    private static KeyPair readKeyPairFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (KeyPair) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Utils.logException(e, "Some error happened in application initialization.");
            return null;
        }
    }

    private static void printMenu() {
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Send post");
        System.out.println("4. See 10 last post");
        System.out.println("5. Send file");
        System.out.println("6. Download file");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
    }

    private static boolean connectToAPI(Socket socket, ObjectInputStream inputStream, ObjectOutputStream outputStream, PublicKey publicKey, PrivateKey privateKey, SecretKey symmetricKey) throws Exception {
        APIPublicKey = (PublicKey) inputStream.readObject();
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

        if (pongValue - 10 == value) {
            Utils.logDebug("Ping pong data validation successful.");
        } else {
            Utils.logDebug("Ping pong data validation failed.");
            Utils.logError("Ping pong data validation failed.");
            cleanResources(inputStream, outputStream, socket);
            return false;
        }

        // get data to validate from API

        byte[] pingRequestAPI = (byte[]) inputStream.readObject();

        Utils.logDebug("Got data to validate from API.");

        int pongValueAPI = Utils.decryptPing(pingRequestAPI, privateKey);

        pongValueAPI += 10;

        Utils.logDebug("Sending pong response to API.");

        outputStream.writeInt(pongValueAPI);
        outputStream.flush();

        int code = inputStream.readInt();

        if (code != 200) {
            Utils.logError("Connection verification gone wrong.");
            cleanResources(inputStream, outputStream, socket);
            return false;
        }

        byte[] key;
        try {
            key = Utils.encryptKey(symmetricKey, APIPublicKey);
        } catch (Exception e) {
            Utils.logException(e, "Could not encrypt symmetric key.");
            cleanResources(inputStream, outputStream, socket);
            return false;
        }

        outputStream.writeObject(new SymmetricKeyMessage(key));

        Utils.logDebug("Sent symmetric key to API.");

        Utils.logInfo("API Gateway is verified server.");

        return true;
    }

    private static void cleanResources(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
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