package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

public class MessageSendingResult {

    public static final String CAUSE_UNKNOWN = "unknown";

    private int statusCode;
    private Throwable failure;
    private boolean loggable = false;
    private Response.Status.Family responseFamily;
    private Optional<Long> retryAfterMillis = Optional.empty();

    public MessageSendingResult(Throwable failure) {
        this.failure = failure;
    }

    public MessageSendingResult(Throwable failure, boolean loggable) {
        this(failure);
        this.loggable = loggable;
    }

    public MessageSendingResult(Result result) {
        this(result.getFailure());
        if (result.getResponse() != null) {
            initializeForStatusCode(result.getResponse().getStatus());
            if (isRetryLater()) {
                initializeRetryAfterMillis(result);
            }
        }
    }

    public MessageSendingResult(int statusCode) {
        initializeForStatusCode(statusCode);
    }

    public MessageSendingResult(int statusCode, long retryAfterMillis) {
        initializeForStatusCode(statusCode);
        if (isRetryLater() && retryAfterMillis >= 0) {
            this.retryAfterMillis = Optional.of(retryAfterMillis);
        }
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean isClientError() {
        return isInFamily(CLIENT_ERROR);
    }

    public boolean isRetryLater() {
        return statusCode == SERVICE_UNAVAILABLE.getStatusCode();
    }

    private void initializeForStatusCode(int statusCode) {
        this.statusCode = statusCode;
        responseFamily = familyOf(statusCode);
        if (this.failure == null && !isInFamily(SUCCESSFUL)) {
            this.failure = new InternalProcessingException("Message sending failed with status code:" + statusCode);
        }
    }

    private void initializeRetryAfterMillis(Result result) {
        HttpFields headers = result.getResponse().getHeaders();
        if (headers.contains(HttpHeader.RETRY_AFTER)) {
            try {
                int seconds = Integer.parseInt(headers.get(HttpHeader.RETRY_AFTER));
                if (seconds >= 0) {
                    retryAfterMillis = Optional.of(TimeUnit.SECONDS.toMillis(seconds));
                }
            } catch (NumberFormatException e) {
                // retryAfterMillis stays empty
            }
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
        return failure != null ? Throwables.getRootCause(failure).getMessage() : CAUSE_UNKNOWN;
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

    public Optional<Long> getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public static MessageSendingResult succeededResult() {
        return new MessageSendingResult(OK.getStatusCode());
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

    public static MessageSendingResult retryAfter(int seconds) {
        return new MessageSendingResult(SERVICE_UNAVAILABLE.getStatusCode(), TimeUnit.SECONDS.toMillis(seconds));
    }
}
