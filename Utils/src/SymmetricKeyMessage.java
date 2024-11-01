import javax.crypto.SecretKey;
import java.io.Serializable;

public class SymmetricKeyMessage implements Serializable {
    public byte[] key;

    public SymmetricKeyMessage(byte[] key) {
        this.key = key;
    }
}
