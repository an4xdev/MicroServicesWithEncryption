import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ApiGatewayThread extends Thread{
    private final Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean debug;
    public ApiGatewayThread(boolean debug, Socket socket) {
        this.debug = debug;
        this.socket = socket;
        prepare();
        start();
    }
    
    private void prepare() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            Utils.logError(true, e, "Error while creating input/output stream");
            cleanResources();
        }
    }
    
    private void cleanResources() {
        try {
            if(inputStream != null)
            {
                inputStream.close();
            }
            if(outputStream != null)
            {
                outputStream.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (Exception e) {
            Utils.logError(debug, e, "Error while closing resources");
        }
    }
    
    @Override
    public void run(){
        while(true) {
            
        }
    }
    
    
}
