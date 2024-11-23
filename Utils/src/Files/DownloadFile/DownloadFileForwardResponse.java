package Files.DownloadFile;

import java.util.UUID;

public class DownloadFileForwardResponse {
    public UUID messageId;
    public int code;
    public String fileName;
    public String message;
    public String fileData;

    public static String ConvertToString(DownloadFileForwardResponse downloadFileForwardResponse) {
        return downloadFileForwardResponse.messageId + ";" + downloadFileForwardResponse.code + ";" + downloadFileForwardResponse.message + ";"+ downloadFileForwardResponse.fileName + ";" + downloadFileForwardResponse.fileData;
    }

    public static DownloadFileForwardResponse ConvertFromString(String downloadFileForwardResponse) {
        String[] parts = downloadFileForwardResponse.split(";");
        DownloadFileForwardResponse newDownloadFileForwardResponse = new DownloadFileForwardResponse();
        newDownloadFileForwardResponse.messageId = UUID.fromString(parts[0]);
        newDownloadFileForwardResponse.code = Integer.parseInt(parts[1]);
        newDownloadFileForwardResponse.message = parts[2];
        newDownloadFileForwardResponse.fileName = parts[3];
        newDownloadFileForwardResponse.fileData = parts[4];
        return newDownloadFileForwardResponse;
    }
}
