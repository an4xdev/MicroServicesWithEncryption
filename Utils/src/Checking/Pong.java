package Checking;

import java.io.Serializable;

public record Pong(byte[] numberValue, byte[] fingerPrint, byte[] encryptedSymmetricKey) implements Serializable {
}
