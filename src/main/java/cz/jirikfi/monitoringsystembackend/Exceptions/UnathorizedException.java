package cz.jirikfi.monitoringsystembackend.Exceptions;

public class UnathorizedException extends RuntimeException {
    public UnathorizedException(String message) {
        super(message);
    }
}
