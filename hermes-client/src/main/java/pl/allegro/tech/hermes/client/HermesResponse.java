package pl.allegro.tech.hermes.client;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;

import java.util.Optional;

public interface HermesResponse {

  String MESSAGE_ID = "Hermes-Message-Id";
  String HTTP_1_1 = "http/1.1";

  int getHttpStatus();

  HermesMessage getHermesMessage();

  @Deprecated
  default boolean wasPublished() {
    return getHttpStatus() == HTTP_CREATED;
  }

  @Deprecated
  default boolean wasAccepted() {
    return wasPublished() || getHttpStatus() == HTTP_ACCEPTED;
  }

  default boolean isSuccess() {
    return getHttpStatus() == HTTP_CREATED || getHttpStatus() == HTTP_ACCEPTED;
  }

  default boolean isFailure() {
    return !isSuccess();
  }

  default Optional<Throwable> getFailureCause() {
    return Optional.empty();
  }

  /**
   * Retrieves failed HermesMessage.
   *
   * @deprecated as of Hermes 1.2.4, in favor of {@link #getHermesMessage()}
   */
  @Deprecated
  default Optional<HermesMessage> getFailedMessage() {
    return Optional.empty();
  }

  default String getBody() {
    return "";
  }

  default String getHeader(String header) {
    return "";
  }

  default String getMessageId() {
    return getHeader(MESSAGE_ID);
  }

  default String getProtocol() {
    return HTTP_1_1;
  }

  default String getDebugLog() {
    StringBuilder builder =
        new StringBuilder("Sending message ")
            .append(getMessageId())
            .append(" to Hermes ")
            .append(isSuccess() ? "succeeded" : "failed")
            .append(", response code: ")
            .append(getHttpStatus())
            .append(", body: ")
            .append(getBody());
    getFailureCause().ifPresent(ex -> builder.append(", exception: ").append(ex.getMessage()));
    return builder.toString();
  }
}
