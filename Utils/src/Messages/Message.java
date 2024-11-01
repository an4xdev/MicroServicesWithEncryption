package Messages;

import java.io.Serializable;
import java.util.Optional;

public class Message implements Serializable {
    public byte[] data;
    public byte[] fingerPrint;

    public Message(byte[] data, byte[] fingerPrint) {
        this.data = data;
        this.fingerPrint = fingerPrint;
    }
}