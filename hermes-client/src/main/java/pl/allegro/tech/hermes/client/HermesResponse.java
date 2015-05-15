package pl.allegro.tech.hermes.client;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;

public interface HermesResponse {

    String MESSAGE_ID = "Hermes-Message-Id";

    int getHttpStatus();

    default boolean wasPublished() {
        return getHttpStatus() == HTTP_CREATED;
    }

    default boolean wasAccepted() {
        return wasPublished() || getHttpStatus() == HTTP_ACCEPTED;
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
