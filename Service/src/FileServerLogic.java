import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileServerLogic implements Runnable{
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public FileServerLogic(ObjectOutputStream out, ObjectInputStream in) {
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
