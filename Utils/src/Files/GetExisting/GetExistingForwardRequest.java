package Files.GetExisting;

import Messages.BaseForwardRequest;

import java.util.UUID;

public class GetExistingForwardRequest extends BaseForwardRequest {

    public static String ConvertToString(GetExistingForwardRequest getExistingForwardRequest) {
        return getExistingForwardRequest.messageId.toString();
    }

    public static GetExistingForwardRequest ConvertFromString(String getExistingForwardRequest) {
        GetExistingForwardRequest newGetExistingForwardRequest = new GetExistingForwardRequest();
        newGetExistingForwardRequest.messageId = UUID.fromString(getExistingForwardRequest);
        return newGetExistingForwardRequest;
    }
}
