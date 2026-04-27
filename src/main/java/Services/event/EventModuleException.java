package Services.event;

public class EventModuleException extends RuntimeException {

    public EventModuleException(String message) {
        super(message);
    }

    public EventModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
