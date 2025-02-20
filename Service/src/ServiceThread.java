import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServiceThread implements Runnable {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final int choice;
    private Thread thread;

    public ServiceThread(Socket socket, int choice) {
        this.socket = socket;
        this.choice = choice;
    }

    private void prepare() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            Utils.logException(e, "Error while creating input/output stream");
            cleanResources();
        }
    }

    private void cleanResources() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Utils.logException(e, "Error while closing resources.");
        }
    }

    public void stop() {
        cleanResources();
        thread.interrupt();
    }

    @Override
    public void run() {
        prepare();
        Runnable logic = switch (choice) {
            case 0 -> new RegisterLogic(out, in);
            case 1 -> new LoginLogic(out, in);
            case 2 -> new ChatLogic(out, in);
            case 3 -> new PostLogic(out, in);
            case 4 -> new FileLogic(out, in);
            default -> throw new IllegalStateException("Unexpected value: " + choice);
        };

        thread = new Thread(logic);
        thread.start();
        var state = thread.getState();
        while (state != Thread.State.TERMINATED) {
            state = thread.getState();
        }
        cleanResources();
    }
}
