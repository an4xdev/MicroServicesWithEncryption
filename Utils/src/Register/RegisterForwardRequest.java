package Register;

import java.io.Serializable;
import java.security.PublicKey;

public class RegisterForwardRequest implements Serializable {
    public String login;
    public PublicKey publicKey;
}
