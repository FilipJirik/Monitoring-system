package cz.jirikfi.monitoringsystembackend.Exceptions;


import java.time.Instant;

public class ApiErrorMessage {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ApiErrorMessage(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }


}
