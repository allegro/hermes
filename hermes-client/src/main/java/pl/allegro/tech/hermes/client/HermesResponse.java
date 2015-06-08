package pl.allegro.tech.hermes.client;

import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;

@FunctionalInterface
public interface HermesResponse {

    String MESSAGE_ID = "Hermes-Message-Id";

    int getHttpStatus();

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

    default String getBody() {
        return "";
    }

    default String getHeader(String header) {
        return "";
    }

    default String getMessageId() {
        return getHeader(MESSAGE_ID);
    }
}
