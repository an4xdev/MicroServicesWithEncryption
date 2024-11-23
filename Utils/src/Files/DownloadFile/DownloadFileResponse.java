package Files.DownloadFile;

import Messages.Message;

public class DownloadFileResponse extends Message {
    public DownloadFileResponse(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
