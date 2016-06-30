package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

public class MessageErrorProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageErrorProcessor.class);
    private final ObjectMapper objectMapper;
    private final Trackers trackers;
    private final HttpString messageIdHeader = new HttpString(MESSAGE_ID.getName());

    @Inject
    public MessageErrorProcessor(ObjectMapper objectMapper, Trackers trackers) {
        this.objectMapper = objectMapper;
        this.trackers = trackers;
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, ErrorDescription error) {
        sendQuietly(exchange, error, messageId);
        log(error, topic, messageId, exchange.getHostAndPort());
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, ErrorDescription error, Exception exception) {
        sendQuietly(exchange, error, messageId);
        log(error, topic, messageId, exchange.getHostAndPort(), exception);
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, Exception e) {
        ErrorDescription error = error("Error while handling request.", INTERNAL_ERROR);
        sendQuietly(exchange, error, messageId);
        log(error, topic, messageId, exchange.getHostAndPort(), e);
    }

    public void sendQuietly(HttpServerExchange exchange, ErrorDescription error, String messageId) {
        try {
            if (exchange.getConnection().isOpen()) {
                send(exchange, error, messageId);
            }
        } catch (Exception e) {
            logger.warn("Exception in sending error response to a client. {} {} {}",
                    error.getMessage(), messageId, exchange.getHostAndPort(), e);
        }
    }

    private void send(HttpServerExchange exchange, ErrorDescription error, String messageId) throws IOException {
        exchange.setStatusCode(error.getCode().getHttpCode());
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        exchange.getResponseHeaders().add(messageIdHeader, messageId);
        exchange.getResponseSender().send(objectMapper.writeValueAsString(error));
    }

    private void log(ErrorDescription error, Topic topic, String messageId, String hostAndPort) {
        logger.error(new StringBuilder()
                .append(error.getMessage())
                .append("; publishing on topic: ")
                .append(topic.getQualifiedName())
                .append("; message id: ")
                .append(messageId)
                .append("; remote host: ")
                .append(hostAndPort)
                .toString());
        trackers.get(topic).logError(messageId, topic.getName(), error.getMessage());
    }

    private void log(ErrorDescription error, Topic topic, String messageId, String hostAndPort, Exception exception) {
        logger.error(new StringBuilder()
                        .append(error.getMessage())
                        .append("; publishing on topic: ")
                        .append(topic.getQualifiedName())
                        .append("; message id: ")
                        .append(messageId)
                        .append("; remote host: ")
                        .append(hostAndPort)
                        .toString(),
                exception);
        trackers.get(topic).logError(messageId, topic.getName(), error.getMessage());
    }
}
