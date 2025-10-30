package cz.jirikfi.monitoringsystembackend.Exceptions;

public class InternalErrorException extends RuntimeException {
    public InternalErrorException(String message) {
        super(message);
    }
}
