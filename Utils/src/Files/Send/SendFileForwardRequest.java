package Files.Send;

import Messages.BaseForwardRequest;

import java.util.UUID;

public class SendFileForwardRequest extends BaseForwardRequest {
    public String fileData;
    public String fileName;

    public static String ConvertToString(SendFileForwardRequest sendFileForwardRequest) {
        return sendFileForwardRequest.messageId + ";" + sendFileForwardRequest.fileData + ";" + sendFileForwardRequest.fileName;
    }

    public static SendFileForwardRequest ConvertFromString(String sendFileForwardRequest) {
        String[] parts = sendFileForwardRequest.split(";");
        SendFileForwardRequest newSendFileForwardRequest = new SendFileForwardRequest();
        newSendFileForwardRequest.messageId = UUID.fromString(parts[0]);
        newSendFileForwardRequest.fileData = parts[1];
        newSendFileForwardRequest.fileName = parts[2];
        return newSendFileForwardRequest;
    }
}
