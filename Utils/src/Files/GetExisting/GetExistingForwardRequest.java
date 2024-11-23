package Files.GetExisting;

import java.io.Serializable;
import java.util.UUID;

public class GetExistingForwardRequest implements Serializable {
    public UUID messageId;

    public static String ConvertToString(GetExistingForwardRequest getExistingForwardRequest) {
        return getExistingForwardRequest.messageId.toString();
    }

    public static GetExistingForwardRequest ConvertFromString(String getExistingForwardRequest) {
        GetExistingForwardRequest newGetExistingForwardRequest = new GetExistingForwardRequest();
        newGetExistingForwardRequest.messageId = UUID.fromString(getExistingForwardRequest);
        return newGetExistingForwardRequest;
    }
}
