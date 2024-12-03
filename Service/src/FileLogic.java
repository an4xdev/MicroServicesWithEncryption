import Files.DownloadFile.DownloadFileForwardRequest;
import Files.DownloadFile.DownloadFileForwardResponse;
import Files.GetExisting.GetExistingForwardRequest;
import Files.GetExisting.GetExistingForwardResponse;
import Files.Send.SendFileForwardRequest;
import Files.Send.SendFileForwardResponse;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;

public class FileLogic implements Runnable {
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public FileLogic(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        String folderName = "uploads";
        File folder = new File(folderName);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                Utils.logDebug("Folder '" + folderName + "' created");
            } else {
                Utils.logError("Could not create folder: '" + folderName + "'.");
                return;
            }
        } else {
            Utils.logDebug("Folder '" + folderName + "' already exists.");
        }

        Object receivedObject;
        while (true) {
            try {
                if ((receivedObject = in.readObject()) == null) break;
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

            if (receivedObject instanceof GetExistingForwardRequest req) {
                Utils.logDebug("Got request about existing files.");
                GetExistingForwardResponse response = new GetExistingForwardResponse();

                ArrayList<String> fileNames = new ArrayList<>();
                File directory = new File(folderName);
                response.messageId = req.messageId;

                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                fileNames.add(file.getName());
                            }
                        }
                    }
                    response.code = 200;
                    response.message = "Files found.";
                    response.fileNames = fileNames;

                } else {
                    System.out.println("Path isn't folder or doesn't exist.");
                    response.code = 404;
                    response.message = "Path isn't folder or doesn't exist.";
                }
                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    Utils.logException(e, "Input/Output operations failed.");
                    break;
                }

            } else if (receivedObject instanceof SendFileForwardRequest req) {
                Utils.logDebug("Got request to send file.");

                SendFileForwardResponse response = new SendFileForwardResponse();
                response.messageId = req.messageId;

                File file = new File(folderName + "/" + req.fileName);
                byte[] fileBytes = Base64.getDecoder().decode(req.fileData);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(fileBytes);
                    response.message = "File saved successfully.";
                    response.code = 200;
                } catch (IOException e) {
                    Utils.logException(e, "Could not save file.");
                    response.message = "Could not save file.";
                    response.code = 500;
                }

                Utils.logDebug("File saved successfully.");

                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    Utils.logException(e, "Input/Output operations failed.");
                    break;
                }

            }
            else if(receivedObject instanceof DownloadFileForwardRequest req) {
                Utils.logDebug("Got request to download file.");
                DownloadFileForwardResponse response = new DownloadFileForwardResponse();
                response.messageId = req.messageId;

                File file = new File(folderName + "/" + req.fileName);

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] fileBytes = fis.readAllBytes();
                    String fileData = Base64.getEncoder().encodeToString(fileBytes);
                    response.code = 200;
                    response.message = "File found.";
                    response.fileName = req.fileName;
                    response.fileData = fileData;
                } catch (IOException e) {
                    Utils.logException(e, "Could not read file.");
                    response.code = 500;
                    response.message = "Could not read file.";
                    response.fileName = req.fileName;
                    response.fileData = "UNKNOWN";
                }

                try {
                    out.writeObject(response);
                    out.flush();
                } catch (IOException e) {
                    Utils.logException(e, "Input/Output operations failed.");
                    break;
                }
            }

            else {
                System.out.println("Unknown message.");
            }
        }
    }
}
