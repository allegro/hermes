package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.end.RemoteHostReader.readHostAndPort;

public class MessageErrorProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageErrorProcessor.class);
    private final ObjectMapper objectMapper;
    private final Trackers trackers;
    private final HttpString messageIdHeader = new HttpString(MESSAGE_ID.getName());

    public MessageErrorProcessor(ObjectMapper objectMapper, Trackers trackers) {
        this.objectMapper = objectMapper;
        this.trackers = trackers;
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, ErrorDescription error) {
        sendQuietly(exchange, error, messageId, topic.getQualifiedName());
        log(error.getMessage(), topic, messageId, readHostAndPort(exchange));
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, ErrorDescription error, Exception exception) {
        sendQuietly(exchange, error, messageId, topic.getQualifiedName());
        log(error.getMessage(), topic, messageId, readHostAndPort(exchange), exception);
    }

    public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, Exception e) {
        ErrorDescription error = error("Error while handling request.", INTERNAL_ERROR);
        sendQuietly(exchange, error, messageId, topic.getQualifiedName());
        log(error.getMessage(), topic, messageId, readHostAndPort(exchange), e);
    }

    public void sendAndLog(HttpServerExchange exchange, String errorMessage, Exception e) {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
        sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(), error(errorMessage, INTERNAL_ERROR), e);
    }

    public void sendQuietly(HttpServerExchange exchange, ErrorDescription error, String messageId, String topicName) {
        try {
            if (exchange.getConnection().isOpen()) {
                if (!exchange.isResponseStarted()) {
                    send(exchange, error, messageId);
                } else {
                    logger.warn("Not sending error message to a client as response has already been started. " +
                                    "Error message: {} Topic: {} MessageId: {} Host: {}",
                            error.getMessage(), topicName, messageId, readHostAndPort(exchange));
                }
            } else {
                logger.warn("Connection to a client closed. Can't send error response. " +
                                "Error message: {} Topic: {} MessageId: {} Host: {}",
                        error.getMessage(), topicName, messageId, readHostAndPort(exchange));
                exchange.endExchange();
            }
        } catch (Exception e) {
            logger.warn("Exception in sending error response to a client. {} Topic: {} MessageId: {} Host: {}",
                    error.getMessage(), topicName, messageId, readHostAndPort(exchange), e);
        }
    }

    public void log(HttpServerExchange exchange, String errorMessage, Exception exception) {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
        log(errorMessage, attachment.getTopic(), attachment.getMessageId(), readHostAndPort(exchange), exception);
    }

    private void send(HttpServerExchange exchange, ErrorDescription error, String messageId) throws IOException {
        exchange.setStatusCode(error.getCode().getHttpCode());
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        exchange.getResponseHeaders().add(messageIdHeader, messageId);
        exchange.getResponseSender().send(objectMapper.writeValueAsString(error), StandardCharsets.UTF_8,
                ResponseReadyIoCallback.INSTANCE);
    }

    private void log(String errorMessage, Topic topic, String messageId, String hostAndPort) {
        logger.error(errorMessage +
                "; publishing on topic: " +
                topic.getQualifiedName() +
                "; message id: " +
                messageId +
                "; remote host: " +
                hostAndPort);
        trackers.get(topic).logError(messageId, topic.getName(), errorMessage, hostAndPort);
    }

    private void log(String errorMessage, Topic topic, String messageId, String hostAndPort, Exception exception) {
        logger.error(errorMessage +
                        "; publishing on topic: " +
                        topic.getQualifiedName() +
                        "; message id: " +
                        messageId +
                        "; remote host: " +
                        hostAndPort,
                exception);
        trackers.get(topic).logError(messageId, topic.getName(), errorMessage, hostAndPort);
    }
}
