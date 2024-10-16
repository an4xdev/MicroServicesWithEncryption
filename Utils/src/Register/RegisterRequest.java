package Register;

import java.io.Serializable;

public record RegisterRequest(byte[] userName,byte[] fingerPrint, byte[] encryptedSymmetricKey) implements Serializable {
}
