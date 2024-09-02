package pl.allegro.tech.hermes.consumers.consumer.sender;

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.TOO_MANY_REQUESTS;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static jakarta.ws.rs.core.Response.Status.Family.familyOf;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.eclipse.jetty.client.Result;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class SingleMessageSendingResult implements MessageSendingResult {

  private Throwable failure;
  private boolean loggable;
  private boolean ignoreInRateCalculation = false;

  private Optional<Long> retryAfterMillis = Optional.empty();
  private Optional<URI> requestUri = Optional.empty();
  private int statusCode;
  private Response.Status.Family responseFamily;

  SingleMessageSendingResult(Throwable failure, boolean ignoreInRateCalculation) {
    this.failure = failure;
    this.loggable = !isTimeout();
    this.ignoreInRateCalculation = ignoreInRateCalculation;
  }

  SingleMessageSendingResult(Throwable failure) {
    this(failure, false);
  }

  SingleMessageSendingResult(Result result) {
    this.failure = result.getFailure();

    if (result.getResponse() != null) {
      initializeForStatusCode(result.getResponse().getStatus());
      if (shouldRetryOnStatusCode()) {
        initializeRetryAfterMillis(result);
      }
    }

    this.loggable = !isTimeout() && !hasHttpAnswer();
    this.requestUri = Optional.ofNullable(result.getRequest().getURI());
  }

  SingleMessageSendingResult(int statusCode) {
    initializeForStatusCode(statusCode);
  }

  SingleMessageSendingResult(int statusCode, URI requestURI) {
    this(statusCode);
    this.requestUri = Optional.of(requestURI);
  }

  SingleMessageSendingResult(int statusCode, long retryAfterMillis) {
    initializeForStatusCode(statusCode);
    if (shouldRetryOnStatusCode() && retryAfterMillis >= 0) {
      this.retryAfterMillis = Optional.of(retryAfterMillis);
    }
  }

  public SingleMessageSendingResult(Result result, URI uri) {
    this(result);
    this.requestUri = Optional.of(uri);
  }

  private void initializeForStatusCode(int statusCode) {
    this.statusCode = statusCode;
    responseFamily = familyOf(statusCode);
    if (this.failure == null && !isInFamily(SUCCESSFUL)) {
      this.failure =
          new InternalProcessingException("Message sending failed with status code: " + statusCode);
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

  @Override
  public boolean isRetryLater() {
    return shouldRetryOnStatusCode() && retryAfterMillis.isPresent();
  }

  private boolean shouldRetryOnStatusCode() {
    return getStatusCode() == SERVICE_UNAVAILABLE.code()
        || getStatusCode() == TOO_MANY_REQUESTS.code();
  }

  @Override
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

  public Optional<URI> getRequestUri() {
    return requestUri;
  }

  @Override
  public boolean isClientError() {
    return isInFamily(CLIENT_ERROR);
  }

  @Override
  public boolean ignoreInRateCalculation(
      boolean retryClientErrors, boolean isOAuthSecuredSubscription) {
    return isRetryLater()
        || this.ignoreInRateCalculation
        || (isClientError()
            && !retryClientErrors
            && !(isOAuthSecuredSubscription && isUnauthorized()));
  }

  private boolean isUnauthorized() {
    return getStatusCode() == UNAUTHORIZED.code();
  }

  @Override
  public boolean isTimeout() {
    return failure instanceof TimeoutException;
  }

  @Override
  public List<MessageSendingResultLogInfo> getLogInfo() {
    return Collections.singletonList(
        new MessageSendingResultLogInfo(getRequestUri(), failure, getRootCause()));
  }

  @Override
  public List<URI> getSucceededUris(Predicate<MessageSendingResult> filter) {
    if (filter.test(this) && requestUri.isPresent()) {
      return Collections.singletonList(getRequestUri().get());
    } else {
      return Collections.emptyList();
    }
  }
}
