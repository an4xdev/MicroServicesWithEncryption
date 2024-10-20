package Checking;

import java.io.Serializable;

public record Ping(byte[] numberValue, byte[] fingerPrint, byte[] encryptedSymmetricKey) implements Serializable {
}
