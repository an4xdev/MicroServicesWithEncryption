package Checking;

import Messages.Message;

import java.io.Serializable;
import java.util.Optional;

public class Ping extends Message {
    public Ping(byte[] numberValue, byte[] fingerPrint, byte[] encryptedSymmetricKey) {
        super(numberValue, fingerPrint, encryptedSymmetricKey);
    }
}
