import java.util.ArrayList;

public class Test {
    public static void main(String[] args) throws Exception {
        ArrayList<String> arguments = new ArrayList<>(){{
            add("java");
            add("-cp");
            add("D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\APIGateway\\out\\production\\APIGateway\\;D:\\PROGRAMOWANIE\\SIECI\\APP\\MicroServicesWithEncryption\\Utils\\out\\artifacts\\Utils_jar\\Utils.jar");
            add("Main");
        }};
        ProcessBuilder builder = new ProcessBuilder(arguments).inheritIO().redirectErrorStream(true);
        var process = builder.start();

        process.waitFor();
    }
}
