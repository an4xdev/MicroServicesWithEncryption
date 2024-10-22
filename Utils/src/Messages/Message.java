package Messages;

import java.io.Serializable;
import java.util.Optional;

public class Message implements Serializable {
    public byte[] data;
    public byte[] fingerPrint;
    public byte[] encryptedSymmetricKey;

    public Message(byte[] data, byte[] fingerPrint, byte[] encryptedSymmetricKey) {
        this.data = data;
        this.fingerPrint = fingerPrint;
        this.encryptedSymmetricKey = encryptedSymmetricKey;
    }
}