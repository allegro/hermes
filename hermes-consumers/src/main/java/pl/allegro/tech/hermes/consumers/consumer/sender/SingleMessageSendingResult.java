package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

public class SingleMessageSendingResult implements MessageSendingResult {

    private Throwable failure;
    private boolean loggable;

    private Optional<Long> retryAfterMillis = Optional.empty();
    private String requestUri = "";
    private int statusCode;
    private Response.Status.Family responseFamily;

    SingleMessageSendingResult(Throwable failure, boolean loggable) {
        this.failure = failure;
        this.loggable = loggable;
    }

    SingleMessageSendingResult(Throwable failure) {
        this.failure = failure;
        this.loggable = !isTimeout(); // Throwable - no status code
    }

    SingleMessageSendingResult(Result result) {
        this.failure = result.getFailure();

        if (result.getResponse() != null) {
            initializeForStatusCode(result.getResponse().getStatus());
            if (isRetryLater()) {
                initializeRetryAfterMillis(result);
            }
        }

        this.loggable = !isTimeout() && !hasHttpAnswer();
        this.requestUri = result.getRequest().getURI().toString();
    }

    SingleMessageSendingResult(int statusCode) {
        initializeForStatusCode(statusCode);
    }

    SingleMessageSendingResult(int statusCode, long retryAfterMillis) {
        initializeForStatusCode(statusCode);
        if (isRetryLater() && retryAfterMillis >= 0) {
            this.retryAfterMillis = Optional.of(retryAfterMillis);
        }
    }

    private void initializeForStatusCode(int statusCode) {
        this.statusCode = statusCode;
        responseFamily = familyOf(statusCode);
        if (this.failure == null && !isInFamily(SUCCESSFUL)) {
            this.failure = new InternalProcessingException("Message sending failed with status code: " + statusCode);
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

    public boolean isRetryLater() {
        return getStatusCode() == SERVICE_UNAVAILABLE.getStatusCode();
    }

    public boolean succeeded() {
        return getFailure() == null;
    }

    Throwable getFailure() {
        return failure;
    }

    @Override
    public String getRootCause() {
        return failure != null ? Throwables.getRootCause(failure).getMessage() : CAUSE_UNKNOWN;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean isLoggable() {
        return loggable;
    }

    @Override
    public Optional<Long> getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public String getRequestUri() {
        return requestUri;
    }

    @Override
    public boolean isClientError() {
        return isInFamily(CLIENT_ERROR);
    }

    public boolean isTimeout() {
        return failure instanceof TimeoutException;
    }

    public List<MessageSendingResultLogInfo> getLogInfo() {
        return Collections.singletonList(new MessageSendingResultLogInfo(requestUri, failure, getRootCause()));
    }

    public List<String> getSucceededUris(Predicate<MessageSendingResult> filter) {
        if(filter.test(this)) {
            return Collections.singletonList(requestUri);
        } else {
            return Collections.emptyList();
        }
    }

}
