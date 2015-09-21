package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.eclipse.jetty.client.api.Result;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import javax.ws.rs.core.Response;

import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;

public class MessageSendingResult {
    public static final String UNKNOWN_CAUSE = "unknown";
    private Throwable failure;
    private boolean loggable = false;
    private Response.Status.Family responseFamily;
    private int statusCode;

    public MessageSendingResult() {
    }

    public MessageSendingResult(Throwable failure) {
        this.failure = failure;
    }

    public MessageSendingResult(Throwable failure, boolean loggable) {
        this.failure = failure;
        this.loggable = loggable;
    }

    public MessageSendingResult(Result result) {
        if (result.isFailed()) {
            this.failure = result.getFailure();
            if (result.getResponse() != null) {
                statusCode = result.getResponse().getStatus();
                responseFamily = familyOf(statusCode);
            }
        } else {
            initializeForStatusCode(result.getResponse().getStatus());
        }
    }

    public MessageSendingResult(int statusCode) {
        initializeForStatusCode(statusCode);
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean isClientError() {
        return isInFamily(CLIENT_ERROR);
    }

    private void initializeForStatusCode(int statusCode) {
        this.statusCode = statusCode;
        responseFamily = familyOf(statusCode);
        if (!isInFamily(SUCCESSFUL)) {
            this.failure = new InternalProcessingException("Message sending failed with status code:" + statusCode);
        }
    }

    private boolean isInFamily(Response.Status.Family family) {
        Preconditions.checkNotNull(family);
        return family.equals(responseFamily);
    }

    public Throwable getFailure() {
        return failure;
    }

    public String getRootCause() {
        return failure != null ? Throwables.getRootCause(failure).getMessage() : UNKNOWN_CAUSE;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean hasHttpAnswer() {
        return getStatusCode() != 0;
    }

    public boolean isTimeout() {
        return getFailure() instanceof TimeoutException;
    }

    public boolean isLoggable() {
        return loggable;
    }

    public static MessageSendingResult succeededResult() {
        return new MessageSendingResult();
    }

    public static MessageSendingResult failedResult(Throwable cause) {
        return new MessageSendingResult(cause);
    }

    public static MessageSendingResult failedResult(int statusCode) {
        return new MessageSendingResult(statusCode);
    }

    public static MessageSendingResult loggedFailResult(Throwable cause) {
        return new MessageSendingResult(cause, true);
    }
}
