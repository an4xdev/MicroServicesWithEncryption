package Checking;

import Messages.Message;

import java.io.Serializable;
import java.util.Optional;

public class Pong extends Message {
    public Pong(byte[] numberValue, byte[] fingerPrint, byte[] encryptedSymmetricKey) {
        super(numberValue, fingerPrint, encryptedSymmetricKey);
    }
}
