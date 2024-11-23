package Files.DownloadFile;

import java.io.Serializable;
import java.util.UUID;

public class DownloadFileForwardRequest implements Serializable {
    public UUID messageId;
    public String fileName;

    public static String ConvertToString(DownloadFileForwardRequest request) {
        return request.messageId.toString() + ";" + request.fileName;
    }

    public static DownloadFileForwardRequest ConvertFromString(String request) {
        String[] parts = request.split(";");
        DownloadFileForwardRequest newRequest = new DownloadFileForwardRequest();
        newRequest.messageId = UUID.fromString(parts[0]);
        newRequest.fileName = parts[1];
        return newRequest;
    }
}
