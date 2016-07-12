package pl.allegro.tech.hermes.consumers.consumer.sender;

import org.eclipse.jetty.client.api.Result;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

public interface MessageSendingResult {
    String CAUSE_UNKNOWN = "unknown";

    static SingleMessageSendingResult succeededResult() {
        return new SingleMessageSendingResult(OK.getStatusCode());
    }

    static SingleMessageSendingResult failedResult(Throwable cause) {
        return new SingleMessageSendingResult(cause);
    }

    static SingleMessageSendingResult failedResult(EndpointAddressResolutionException cause) {
        return new SingleMessageSendingResult(cause, cause.isIgnoreInRateCalculation());
    }

    static SingleMessageSendingResult failedResult(int statusCode) {
        return new SingleMessageSendingResult(statusCode);
    }

    static SingleMessageSendingResult ofStatusCode(int statusCode) {
        return new SingleMessageSendingResult(statusCode);
    }

    static SingleMessageSendingResult retryAfter(int seconds) {
        return new SingleMessageSendingResult(SERVICE_UNAVAILABLE.getStatusCode(), TimeUnit.SECONDS.toMillis(seconds));
    }

    static SingleMessageSendingResult of(Result result) {
        return new SingleMessageSendingResult(result);
    }

    String getRootCause();

    int getStatusCode();

    boolean isLoggable();

    Optional<Long> getRetryAfterMillis();

    boolean isClientError();

    boolean isTimeout();

    boolean succeeded();

    boolean ignoreInRateCalculation(boolean retryClientErrors, boolean isOAuthSecuredSubscription);

    default boolean hasHttpAnswer() {
        return getStatusCode() != 0;
    }

    boolean isRetryLater();

    List<MessageSendingResultLogInfo> getLogInfo();

    List<String> getSucceededUris(Predicate<MessageSendingResult> filter) ;
}
