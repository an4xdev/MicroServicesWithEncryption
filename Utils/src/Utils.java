import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class Utils {

    public static final boolean debug = true;

    public static enum Codes {
        KeyError,
        SecretKeyError
    }

    public static enum Ports {
        ApiGateway(21000),
        Register(21001),
        Login(21002),
        Chat(21003),
        Posts(21004),
        FileServer(21005);

        private final int port;

        Ports(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }
    }

    public static final String RESET = "\033[0m";
    public static final String RED = "\033[0;31m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";


    public static void logInfo(String message) {
        System.out.println(BLUE + " INFO: " + RESET + message);
    }

    public static void logDebug(String message) {
        if (debug) {
            System.out.println(YELLOW + "DEBUG: " + RESET + message);
        }
    }

    public static void logError(String message) {
        System.out.println(RED + "ERROR: " + RESET + message);
    }

    public static void logException(Exception e, String message) {
        if (debug) {
            e.printStackTrace();
            System.err.println("-----------------------------");
            System.err.println(RED + "ERROR: " + RESET + e.getMessage());
        } else {
            System.err.println(RED + "ERROR: " + RESET + message);
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public static byte[] hashData(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] signData(byte[] hashedData, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(hashedData);
        return signature.sign();
    }

    public static boolean verifySignature(byte[] hashedData, byte[] signedHash, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(hashedData);
        return verifier.verify(signedHash);
    }

    public static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    public static byte[] encryptPing(int value, PublicKey receiverPubKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, receiverPubKey);
        return cipher.doFinal(Integer.toString(value).getBytes(StandardCharsets.UTF_8));
    }

    public static int decryptPing(byte[] encryptedValue, PrivateKey receiverPrivateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NumberFormatException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, receiverPrivateKey);
        byte[] decryptedValue = cipher.doFinal(encryptedValue);
        return Integer.parseInt(new String(decryptedValue));
    }

    public static byte[] encrypt(byte[] dataToEncrypt, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(dataToEncrypt);
    }

    public static byte[] decrypt(byte[] dataToDecrypt, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(dataToDecrypt);
    }

    public static byte[] encryptKey(SecretKey secretKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(secretKey.getEncoded());
    }

    public static SecretKey decryptKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
    }

    public static <T> Operation sendMessage(Class<T> clazz, ObjectOutputStream outputStream, String data, PrivateKey privateKey, SecretKey symmetricKey) {
        // Data hashing
        byte[] hashedData = null;
        try {
            hashedData = Utils.hashData(data);
        } catch (NoSuchAlgorithmException e) {
            return new Operation(false, "Could not hash data.");
        }

        // Signing with private key
        byte[] singedHash = null;
        try {
            singedHash = Utils.signData(hashedData, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return new Operation(false, "Could not sign data.");
        }

        // Encrypting data with symmetric key
        byte[] dataWithSymetricKey = null;
        try {
            dataWithSymetricKey = Utils.encrypt(data.getBytes(StandardCharsets.UTF_8), symmetricKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not encrypt data.");
        }

        // Encrypting signature with symmetric key
        byte[] fingerprintWithSymmetricKey;
        try {
            fingerprintWithSymmetricKey = Utils.encrypt(singedHash, symmetricKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            return new Operation(false, "Could not encrypt fingerprint.");
        }

        Constructor<T> constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor(byte[].class, byte[].class);
        } catch (NoSuchMethodException e) {
            return new Operation(false, "Could not get constructor.");
        }

        T message = null;
        try {
            message = constructor.newInstance(dataWithSymetricKey, fingerprintWithSymmetricKey);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return new Operation(false, "Could not create message.");
        }

        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            return new Operation(false, "Could not send message.");
        }

        return new Operation(true, "Message sent successfully.");
    }

    public static Operation processMessage(byte[] dataWithSymmetricKey, byte[] fingerprintWithSymmetricKey, PublicKey senderPublicKey, SecretKey symmetricKey) {
        byte[] data;
        try {
            data = Utils.decrypt(dataWithSymmetricKey, symmetricKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            Utils.logException(e, "Could not decrypt data.");
            return new Operation(false, "Could not decrypt data.");
        }

        byte[] fingerprint;
        try {
            fingerprint = Utils.decrypt(fingerprintWithSymmetricKey, symmetricKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            Utils.logException(e, "Could not decrypt fingerprint.");
            return new Operation(false, "Could not decrypt fingerprint.");
        }

        byte[] hashedData;
        try {
            hashedData = Utils.hashData(new String(data));
        } catch (NoSuchAlgorithmException e) {
            Utils.logException(e, "Could not hash data.");
            return new Operation(false, "Could not hash data.");
        }

        boolean isSignatureValid;
        try {
            isSignatureValid = Utils.verifySignature(hashedData, fingerprint, senderPublicKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            Utils.logException(e, "Could not verify signature.");
            return new Operation(false, "Could not verify signature.");
        }

        if (isSignatureValid) {
            String stringData = new String(data);
            logDebug("Signature is valid. Message is authentic.");
            logDebug("Decrypted message: " + stringData);
            return new Operation(true, stringData);
        }

        return new Operation(false, "Signature is invalid! Message could have been modified.");
    }
}
