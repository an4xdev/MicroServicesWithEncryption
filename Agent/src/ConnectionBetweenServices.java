import java.time.LocalDateTime;
import java.util.UUID;

public class ConnectionBetweenServices {
    private final String sourceService;
    private final UUID sourceServiceId;
    private final String targetService;
    private final UUID targetServiceId;
    private LocalDateTime lastActivity;
    private boolean isClosing = false;

    public ConnectionBetweenServices(
            String sourceService, UUID sourceServiceId,
            String targetService, UUID targetServiceId
    ) {
        this.sourceService = sourceService;
        this.sourceServiceId = sourceServiceId;
        this.targetService = targetService;
        this.targetServiceId = targetServiceId;
    }

    public void updateActivity() {
        lastActivity = LocalDateTime.now();
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public UUID getTargetServiceId() {
        return targetServiceId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public UUID getSourceServiceId() {
        return sourceServiceId;
    }

    public String getTargetService() {
        return targetService;
    }

    public boolean isClosing() {
        return isClosing;
    }

    public void setClosing(boolean closing) {
        isClosing = closing;
    }
}
