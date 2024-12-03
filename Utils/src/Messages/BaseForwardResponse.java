package Messages;

import java.io.Serializable;
import java.util.UUID;

public class BaseForwardResponse implements Serializable {
    public UUID messageId;
    public int code;
    public String message;
}
