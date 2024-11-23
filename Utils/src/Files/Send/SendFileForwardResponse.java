package Files.Send;

import java.io.Serializable;
import java.util.UUID;

public class SendFileForwardResponse implements Serializable {
    public UUID messageId;
    public int code;
    public String message;

    public static String ConvertToString(SendFileForwardResponse sendFileForwardResponse) {
        return sendFileForwardResponse.messageId + ";" + sendFileForwardResponse.code + ";" + sendFileForwardResponse.message;
    }

    public static SendFileForwardResponse ConvertFromString(String sendFileForwardResponse) {
        String[] parts = sendFileForwardResponse.split(";");
        SendFileForwardResponse newSendFileForwardResponse = new SendFileForwardResponse();
        newSendFileForwardResponse.messageId = UUID.fromString(parts[0]);
        newSendFileForwardResponse.code = Integer.parseInt(parts[1]);
        newSendFileForwardResponse.message = parts[2];
        return newSendFileForwardResponse;
    }
}
