import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatLogic implements Runnable{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public ChatLogic(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {

    }
}
