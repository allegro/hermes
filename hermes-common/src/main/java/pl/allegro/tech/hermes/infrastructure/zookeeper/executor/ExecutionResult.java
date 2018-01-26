package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

public class ExecutionResult {
    private final Status status;
    private final Exception exception;

    private ExecutionResult(Status status, Exception exception) {
        this.status = status;
        this.exception = exception;
    }

    public Status getStatus() {
        return status;
    }

    public Exception getException() {
        return exception;
    }

    public static ExecutionResult success() {
        return new ExecutionResult(Status.SUCCESS, null);
    }

    public static ExecutionResult failure(Exception exception) {
        return new ExecutionResult(Status.FAILURE, exception);
    }

    public enum Status {
        SUCCESS, FAILURE
    }
}
