package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.end.RemoteHostReader.readHostAndPort;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

public class MessageEndProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MessageEndProcessor.class);
  private static final HttpString messageIdHeader = new HttpString(MESSAGE_ID.getName());

  private final Trackers trackers;
  private final TrackingHeadersExtractor trackingHeadersExtractor;

  public MessageEndProcessor(Trackers trackers, TrackingHeadersExtractor trackingHeadersExtractor) {
    this.trackers = trackers;
    this.trackingHeadersExtractor = trackingHeadersExtractor;
  }

  public void eachSent(
      HttpServerExchange exchange, AttachmentContent attachment, String datacenter) {
    trackers
        .get(attachment.getTopic())
        .logPublished(
            attachment.getMessageId(),
            attachment.getTopic().getName(),
            readHostAndPort(exchange),
            datacenter,
            trackingHeadersExtractor.extractHeadersToLog(exchange.getRequestHeaders()));
  }

  public void sent(HttpServerExchange exchange, AttachmentContent attachment) {
    sendResponse(exchange, attachment, StatusCodes.CREATED);
  }

  private void sendResponse(
      HttpServerExchange exchange, AttachmentContent attachment, int statusCode) {
    if (!exchange.isResponseStarted()) {
      exchange.setStatusCode(statusCode);
      exchange.getResponseHeaders().add(messageIdHeader, attachment.getMessageId());
    } else {
      logger
          .atWarn()
          .addKeyValue(TOPIC_NAME, attachment.getCachedTopic().getQualifiedName())
          .log(
              "The response has already been started. Status code set on exchange: {}; "
                  + "Expected status code: {}; Topic: {}; Message id: {}; Remote host {}",
              exchange.getStatusCode(),
              statusCode,
              attachment.getCachedTopic().getQualifiedName(),
              attachment.getMessageId(),
              readHostAndPort(exchange));
    }
    attachment.markResponseAsReady();
    try {
      exchange.endExchange();
    } catch (RuntimeException exception) {
      logger
          .atError()
          .addKeyValue(TOPIC_NAME, attachment.getCachedTopic().getQualifiedName())
          .setCause(exception)
          .log(
              "Exception while ending exchange. Status code set on exchange: {}; "
                  + "Expected status code: {}; Topic: {}; Message id: {}; Remote host {}",
              exchange.getStatusCode(),
              statusCode,
              attachment.getCachedTopic().getQualifiedName(),
              attachment.getMessageId(),
              readHostAndPort(exchange),
              exception);
    }
  }
}
