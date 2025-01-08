package pl.allegro.tech.hermes.consumers.consumer.sender;

import static io.netty.handler.codec.http.HttpResponseStatus.TOO_MANY_REQUESTS;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static java.util.stream.Collectors.joining;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.eclipse.jetty.client.Result;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;

public interface MessageSendingResult {
  String CAUSE_UNKNOWN = "unknown";

  static SingleMessageSendingResult succeededResult() {
    return new SingleMessageSendingResult(OK.getStatusCode());
  }

  static SingleMessageSendingResult succeededResult(URI requestURI) {
    return new SingleMessageSendingResult(OK.getStatusCode(), requestURI);
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
    return new SingleMessageSendingResult(
        SERVICE_UNAVAILABLE.getStatusCode(), TimeUnit.SECONDS.toMillis(seconds));
  }

  static SingleMessageSendingResult tooManyRequests(int seconds) {
    return new SingleMessageSendingResult(
        TOO_MANY_REQUESTS.code(), TimeUnit.SECONDS.toMillis(seconds));
  }

  static SingleMessageSendingResult of(Result result) {
    return new SingleMessageSendingResult(result);
  }

  static SingleMessageSendingResult ofResultWithUri(Result result, URI uri) {
    return new SingleMessageSendingResult(result, uri);
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

  List<URI> getSucceededUris(Predicate<MessageSendingResult> filter);

  default String getHostname() {
    return getLogInfo().stream()
        .filter(logInfo -> logInfo.getUrl().isPresent())
        .map(logInfo -> logInfo.getUrl().get().getHost())
        .collect(joining(","));
  }
}
