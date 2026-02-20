package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.end.RemoteHostReader.readHostAndPort;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

public class MessageErrorProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MessageErrorProcessor.class);
  private static final Set<ErrorCode> CLIENT_ERRORS =
      Set.of(ErrorCode.VALIDATION_ERROR, ErrorCode.THROUGHPUT_QUOTA_VIOLATION, ErrorCode.TIMEOUT);

  private final ObjectMapper objectMapper;
  private final Trackers trackers;
  private final HttpString messageIdHeader = new HttpString(MESSAGE_ID.getName());
  private final TrackingHeadersExtractor trackingHeadersExtractor;

  public MessageErrorProcessor(
      ObjectMapper objectMapper,
      Trackers trackers,
      TrackingHeadersExtractor trackingHeadersExtractor) {
    this.objectMapper = objectMapper;
    this.trackers = trackers;
    this.trackingHeadersExtractor = trackingHeadersExtractor;
  }

  public void sendAndLog(
      HttpServerExchange exchange, Topic topic, String messageId, ErrorDescription error) {
    sendQuietly(exchange, error, messageId, topic.getQualifiedName());
    log(
        error.getMessage(),
        topic,
        messageId,
        readHostAndPort(exchange),
        levelFor(error),
        null,
        trackingHeadersExtractor.extractHeadersToLog(exchange.getRequestHeaders()));
  }

  public void sendAndLog(
      HttpServerExchange exchange,
      Topic topic,
      String messageId,
      ErrorDescription error,
      Exception exception) {
    sendQuietly(exchange, error, messageId, topic.getQualifiedName());
    log(
        error.getMessage(),
        topic,
        messageId,
        readHostAndPort(exchange),
        levelFor(error),
        exception,
        trackingHeadersExtractor.extractHeadersToLog(exchange.getRequestHeaders()));
  }

  public void sendAndLog(HttpServerExchange exchange, Topic topic, String messageId, Exception e) {
    ErrorDescription error = error("Error while handling request.", INTERNAL_ERROR);
    sendQuietly(exchange, error, messageId, topic.getQualifiedName());
    log(
        error.getMessage(),
        topic,
        messageId,
        readHostAndPort(exchange),
        Level.ERROR,
        e,
        trackingHeadersExtractor.extractHeadersToLog(exchange.getRequestHeaders()));
  }

  public void sendAndLog(HttpServerExchange exchange, String errorMessage, Exception e) {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
    sendAndLog(
        exchange,
        attachment.getTopic(),
        attachment.getMessageId(),
        error(errorMessage, INTERNAL_ERROR),
        e);
  }

  public void sendQuietly(
      HttpServerExchange exchange, ErrorDescription error, String messageId, String topicName) {
    LoggingEventBuilder warnLogger = logger.atWarn().addKeyValue(TOPIC_NAME, topicName);

    try {
      if (exchange.getConnection().isOpen()) {
        if (!exchange.isResponseStarted()) {
          send(exchange, error, messageId);
        } else {
          warnLogger.log(
              "Not sending error message to a client as response has already been started. "
                  + "Error message: {} Topic: {} MessageId: {} Host: {}",
              error.getMessage(),
              topicName,
              messageId,
              readHostAndPort(exchange));
        }
      } else {
        warnLogger.log(
            "Connection to a client closed. Can't send error response. "
                + "Error message: {} Topic: {} MessageId: {} Host: {}",
            error.getMessage(),
            topicName,
            messageId,
            readHostAndPort(exchange));
        exchange.endExchange();
      }
    } catch (Exception e) {
      warnLogger
          .setCause(e)
          .log(
              "Exception in sending error response to a client. {} Topic: {} MessageId: {} Host: {}",
              error.getMessage(),
              topicName,
              messageId,
              readHostAndPort(exchange));
    }
  }

  public void log(HttpServerExchange exchange, String errorMessage, Exception exception) {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
    log(
        errorMessage,
        attachment.getTopic(),
        attachment.getMessageId(),
        readHostAndPort(exchange),
        Level.ERROR,
        exception,
        trackingHeadersExtractor.extractHeadersToLog(exchange.getRequestHeaders()));
  }

  private void log(
      String errorMessage,
      Topic topic,
      String messageId,
      String hostAndPort,
      Level level,
      Exception exception,
      Map<String, String> extraRequestHeaders) {
    LoggingEventBuilder logBuilder =
        logger.atLevel(level).addKeyValue(TOPIC_NAME, topic.getQualifiedName());
    if (exception != null) {
      logBuilder = logBuilder.setCause(exception);
    }
    logBuilder.log(
        "{}; publishing on topic: {}; message id: {}; remote host: {}",
        errorMessage,
        topic.getQualifiedName(),
        messageId,
        hostAndPort);
    trackers
        .get(topic)
        .logError(messageId, topic.getName(), errorMessage, hostAndPort, extraRequestHeaders);
  }

  private Level levelFor(ErrorDescription error) {
    return CLIENT_ERRORS.contains(error.getCode()) ? Level.WARN : Level.ERROR;
  }

  private void send(HttpServerExchange exchange, ErrorDescription error, String messageId)
      throws IOException {
    exchange.setStatusCode(error.getCode().getHttpCode());
    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    exchange.getResponseHeaders().add(messageIdHeader, messageId);
    exchange
        .getResponseSender()
        .send(
            objectMapper.writeValueAsString(error),
            StandardCharsets.UTF_8,
            ResponseReadyIoCallback.INSTANCE);
  }
}
