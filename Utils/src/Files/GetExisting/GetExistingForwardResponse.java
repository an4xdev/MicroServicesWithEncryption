package Files.GetExisting;

import Messages.BaseForwardResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class GetExistingForwardResponse extends BaseForwardResponse {
    public ArrayList<String> fileNames;

    public static String ConvertToString(GetExistingForwardResponse getExistingForwardResponse) {
        StringBuilder fileNames = new StringBuilder();
        for (String fileName : getExistingForwardResponse.fileNames) {
            fileNames.append(fileName).append("~");
        }
        return getExistingForwardResponse.messageId + ";" + getExistingForwardResponse.code + ";" + getExistingForwardResponse.message + ";" + fileNames;
    }

    public static GetExistingForwardResponse ConvertFromString(String getExistingForwardResponse) {
        String[] parts = getExistingForwardResponse.split(";");
        GetExistingForwardResponse newGetExistingForwardResponse = new GetExistingForwardResponse();
        newGetExistingForwardResponse.messageId = UUID.fromString(parts[0]);
        newGetExistingForwardResponse.code = Integer.parseInt(parts[1]);
        newGetExistingForwardResponse.message = parts[2];
        newGetExistingForwardResponse.fileNames = new ArrayList<>();
        String[] fileNames = parts[3].split("~");
        newGetExistingForwardResponse.fileNames.addAll(Arrays.asList(fileNames));
        return newGetExistingForwardResponse;
    }
}
