import Agent.Requests.ConnectToService;
import Agent.Requests.SentData;
import Agent.Responses.ConnectData;
import Enums.Ports;
import Enums.Services;
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
import Messages.BaseForwardRequest;
import Messages.Message;
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class ApiGatewayThread implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream clientInputStream;
    private ObjectOutputStream clientOutputStream;

    private final KeyPair keyPair;
    private PublicKey clientPublicKey;
    private SecretKey symmetricKey;

    private final ConnectionToAgent connectionToAgent;
    private final HashMap<Services, ArrayList<ConnectionToService>> connections;
    private final UUID apiGatewayId;

    public ApiGatewayThread(Socket socket, KeyPair keyPair, UUID apiGatewayId,
                            ConnectionToAgent connectionToAgent,
                            HashMap<Services,
                                    ArrayList<ConnectionToService>> connections) {
        this.clientSocket = socket;
        this.keyPair = keyPair;
        this.apiGatewayId = apiGatewayId;
        this.connectionToAgent = connectionToAgent;
        this.connections = connections;
    }

    private void prepare() {
        try {
            clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void initialConnection() {
        try {
            clientOutputStream.writeObject(keyPair.getPublic());
            clientOutputStream.flush();
        } catch (IOException e) {
            cleanResources();
        }
        Utils.logDebug("Sent public key to client");

        clientPublicKey = null;
        try {
            clientPublicKey = (PublicKey) clientInputStream.readObject();
        } catch (IOException e) {
            if (e instanceof EOFException) {
                System.out.println("Connection closed by client.");
            } else {
                Utils.logException(e, "Input/Output operations failed.");
            }
            cleanResources();
        } catch (ClassNotFoundException e) {
            Utils.logException(e, "Unknown message.");
            cleanResources();
        }
        Utils.logDebug("Got client public key");
    }

    private void cleanResources() {
        try {
            if (clientInputStream != null) {
                clientInputStream.close();
            }
            if (clientOutputStream != null) {
                clientOutputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    private Operation processPing() {

        Utils.logDebug("Processing ping pong operation.");
        // got ping from client
        byte[] objRequest = null;
        try {
            objRequest = (byte[]) clientInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new Operation(false, "Could not read object.");
        }

        // decrypting ping (client is checking if server has the private key)

        int value;
        try {
            value = Utils.decryptPing(objRequest, keyPair.getPrivate());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not decrypt ping request.");
        }

        // sending pong to client

        Utils.logDebug("Sending pong response to client.");

        value += 10;

        try {
            clientOutputStream.writeInt(value);
        } catch (IOException e) {
            return new Operation(false, "Could not send pong response.");
        }

        Utils.logDebug("Sending ping request to client.");

        // sending ping to client(API is checking if client has the private key)

        int valueAPI = new Random().nextInt();

        byte[] pingRequestAPI;
        try {
            pingRequestAPI = Utils.encryptPing(valueAPI, clientPublicKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not encrypt ping request.");
        }

        try {
            clientOutputStream.writeObject(pingRequestAPI);
        } catch (IOException e) {
            return new Operation(false, "Could not send ping request to client.");
        }

        // got pong from client

        int pongValueClient;
        try {
            pongValueClient = clientInputStream.readInt();
        } catch (IOException e) {
            return new Operation(false, "Could not read pong response from Client.");
        }

        Utils.logDebug("Got pong response from client.");

        // validating pong from client

        if (pongValueClient - 10 != valueAPI) {
            try {
                clientOutputStream.writeInt(400);
                clientOutputStream.flush();
            } catch (IOException e) {
                Utils.logException(e, "Sending error status code to Client failed.");
                return new Operation(false, "Sending error status code to Client failed.");
            }
            Utils.logError("Ping pong data validation failed.");
            return new Operation(false, "Ping pong data validation failed.");
        }

        try {
            clientOutputStream.writeInt(200);
            clientOutputStream.flush();
        } catch (IOException e) {
            return new Operation(false, "Could not send final response to client.");
        }

        Utils.logDebug("Ping pong data validation successful.");
        return new Operation(true, "");

    }

    private Operation sendToService(BaseForwardRequest request, Services service) {

        ConnectionToService serviceRunnable = null;

        synchronized (connections) {
            var list = connections.get(service);
            if (!list.isEmpty()) {
                serviceRunnable = list.getFirst();
            }
            connections.notifyAll();
        }

        if (serviceRunnable == null) {
            Utils.logDebug("No connection to service. Creating new connection.");
            var connectionRequest = new ConnectToService(request.messageId, service);
            connectionToAgent.sendMessageToAgent(connectionRequest);

            ConnectData response;
            try {
                response = connectionToAgent.receiveMessageFromAgent(request.messageId, ConnectData.class);
            } catch (InterruptedException e) {
                return new Operation(false, "Could not receive response from agent.");
            }

            Utils.logDebug("Got connection data from agent.");

            Utils.logDebug("HOST: " + response.host + " PORT: " + response.port + " SERVICE ID: " + response.serviceId + " SERVICE NAME: " + response.serviceName + " IS EQUAL: " + (Ports.Agent.getPort() == response.port));

            var connectionToService = new ConnectionToService(
                    response.port, response.host,
                    apiGatewayId,
                    response.serviceId, response.serviceName,
                    connectionToAgent);

            Utils.logDebug("Starting connection to service.");

            synchronized (connections) {
                connections.get(service).add(connectionToService);
                connections.notifyAll();
            }

//            connectionToService.run();

            new Thread(connectionToService).start();

            serviceRunnable = connectionToService;

            while (!serviceRunnable.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Utils.logException(e, "Error while sleeping.");
                }
            }
        }

        Utils.logDebug("Sending message to service.");

        serviceRunnable.sendMessageToService(request);

        connectionToAgent.sendMessageToAgent(
                new SentData(
                        UUID.randomUUID(),
                        serviceRunnable.getTargetServiceId()
                )
        );

        return new Operation(true, "");
    }

    private <T> T receiveMessage(UUID messageId, Class<T> clazz, Services service) {

        ConnectionToService serviceRunnable = null;

        synchronized (connections) {
            var list = connections.get(service);
            if (!list.isEmpty()) {
                serviceRunnable = list.getFirst();
            }
            connections.notifyAll();
        }

        if (serviceRunnable == null) {
            return null;
        }

        try {
            T messageFromService = serviceRunnable.getMessageFromService(messageId, clazz);
            Utils.logDebug("Got message from service.");
            return messageFromService;
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void run() {
        prepare();
        initialConnection();
        var pingOperation = processPing();
        if (!pingOperation.isSuccessful()) {
            Utils.logError("Could not process ping pong operation: " + pingOperation.message());
            cleanResources();
            return;
        }
        Object receivedObject;
        label:
        while (true) {
            try {
                if ((receivedObject = clientInputStream.readObject()) == null) break;
            } catch (IOException e) {
                if (e instanceof EOFException) {
                    Utils.logInfo("Connection closed by client.");
                } else {
                    Utils.logException(e, "Input/Output operations failed.");
                }
                break;
            } catch (ClassNotFoundException e) {
                Utils.logException(e, "Unknown message.");
                break;
            }

            if (receivedObject instanceof SymmetricKeyMessage message) {
                try {
                    symmetricKey = Utils.decryptKey(message.key, keyPair.getPrivate());
                } catch (Exception e) {
                    Utils.logException(e, "Could not decrypt symmetric key.");
                    break;
                }
                Utils.logDebug("Got symmetric key.");
            } else if (receivedObject instanceof Message mem) {
                byte[] dataWithSymmetricKey = mem.data;
                byte[] fingerprintWithSymmetricKey = mem.fingerPrint;
                switch (receivedObject) {
                    case RegisterRequest _ -> {
                        Utils.logDebug("Got register request");

                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = RegisterForwardRequest.ConvertToRegisterForwardRequest(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.Register);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        RegisterForwardResponse response = receiveMessage(request.messageId, RegisterForwardResponse.class, Services.Register);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(RegisterResponse.class, clientOutputStream, RegisterForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }

                    }
                    case LoginRequest _ -> {
                        Utils.logDebug("Got Login request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = LoginForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.Login);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        LoginForwardResponse response = receiveMessage(request.messageId, LoginForwardResponse.class, Services.Login);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(LoginResponse.class, clientOutputStream, LoginForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }
                    }
                    case AddPostRequest _ -> {
                        Utils.logDebug("Got add post request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = AddPostForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.Chat);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        AddPostForwardResponse response = receiveMessage(request.messageId, AddPostForwardResponse.class, Services.Chat);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(AddPostResponse.class, clientOutputStream, AddPostForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }
                    }
                    case GetPostsRequest _ -> {
                        Utils.logDebug("Got retrieve post request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = GetPostsForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.Posts);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        GetPostsForwardResponse response = receiveMessage(request.messageId, GetPostsForwardResponse.class, Services.Posts);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(GetPostsResponse.class, clientOutputStream, GetPostsForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }

                    }
                    case SendFileRequest _ -> {
                        Utils.logDebug("Got send file request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = SendFileForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.File);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        SendFileForwardResponse response = receiveMessage(request.messageId, SendFileForwardResponse.class, Services.File);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(SendFileResponse.class, clientOutputStream, SendFileForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }
                    }
                    case GetExistingRequest _ -> {
                        Utils.logDebug("Got get existing files request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = GetExistingForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.File);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        GetExistingForwardResponse response = receiveMessage(request.messageId, GetExistingForwardResponse.class, Services.File);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(GetExistingResponse.class, clientOutputStream, GetExistingForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }
                    }
                    case DownloadFileRequest _ -> {
                        Utils.logDebug("Got download file request");
                        var operation = Utils.processMessage(dataWithSymmetricKey, fingerprintWithSymmetricKey, clientPublicKey, symmetricKey);

                        if (!operation.isSuccessful()) {
                            Utils.logDebug(operation.message());
                            Utils.logInfo(operation.message());
                            break label;
                        }

                        var request = DownloadFileForwardRequest.ConvertFromString(operation.message());

                        var sendToServiceOperation = sendToService(request, Services.File);
                        if (!sendToServiceOperation.isSuccessful()) {
                            Utils.logError(sendToServiceOperation.message());
                            break label;
                        }

                        DownloadFileForwardResponse response = receiveMessage(request.messageId, DownloadFileForwardResponse.class, Services.File);

                        if (response == null) {
                            Utils.logError("Could not receive response from service.");
                            break label;
                        }

                        var responseOperation = Utils.sendMessage(DownloadFileResponse.class, clientOutputStream, DownloadFileForwardResponse.ConvertToString(response), keyPair.getPrivate(), symmetricKey);

                        if (!responseOperation.isSuccessful()) {
                            Utils.logError(responseOperation.message());
                        }
                    }
                    default -> {
                        System.out.println("Unknown message.");
                        cleanResources();
                        break label;
                    }
                }
            } else {
                System.out.println("Unknown data.");
            }
        }

        cleanResources();
    }
}
