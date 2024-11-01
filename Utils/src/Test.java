public class Test {
    public static void main(String[] args) throws Exception {
        String data = "A";

        byte[] hData = Utils.hashData(data);

        byte[] signature = Utils.signData(hData, Utils.generateKeyPair().getPrivate());

        System.out.println("Data: " + new String(hData));
        System.out.println("Signature: " + new String(signature));
    }
}
