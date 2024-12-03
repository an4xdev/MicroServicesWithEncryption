package Messages;

import java.io.Serializable;

public class Message implements Serializable {
    public byte[] data;
    public byte[] fingerPrint;

    public Message(byte[] data, byte[] fingerPrint) {
        this.data = data;
        this.fingerPrint = fingerPrint;
    }
}