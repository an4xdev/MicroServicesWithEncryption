package Files.DownloadFile;

import Messages.Message;

public class DownloadFileRequest extends Message {
    public DownloadFileRequest(byte[] data, byte[] fingerPrint) {
        super(data, fingerPrint);
    }
}
