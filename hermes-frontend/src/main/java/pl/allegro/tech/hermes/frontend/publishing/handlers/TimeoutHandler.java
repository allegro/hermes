package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.SENDING_TO_KAFKA_TIMEOUT;
import static pl.allegro.tech.hermes.api.ErrorCode.TIMEOUT;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

class TimeoutHandler implements HttpHandler {

  private final MessageErrorProcessor messageErrorProcessor;
  private final MessageEndProcessor messageEndProcessor;
  private static final Logger logger = LoggerFactory.getLogger(TimeoutHandler.class);

  TimeoutHandler(
      MessageEndProcessor messageEndProcessor, MessageErrorProcessor messageErrorProcessor) {
    this.messageErrorProcessor = messageErrorProcessor;
    this.messageEndProcessor = messageEndProcessor;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
    MessageState state = attachment.getMessageState();

    if (state.setReadingTimeout()) {
      readingTimeout(exchange, attachment);
    } else if (state.setTimeoutSendingToKafka()) {
      sendingToKafkaTimeout(exchange, attachment);
    } else {
      state.setPrematureTimeout();
    }
  }

  private void readingTimeout(HttpServerExchange exchange, AttachmentContent attachment) {
    exchange
        .getConnection()
        .getWorker()
        .execute(
            () -> {
              TimeoutHolder timeoutHolder = attachment.getTimeoutHolder();

              if (timeoutHolder != null) {
                timeoutHolder.timeout();
                messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error(
                        "Timeout while reading message after "
                            + timeoutHolder.getTimeout()
                            + " milliseconds",
                        TIMEOUT));
              } else {
                messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error(
                        "Probably context switching problem as timeout task was started before it was attached to an exchange",
                        INTERNAL_ERROR));
              }
            });
  }

  private void sendingToKafkaTimeout(HttpServerExchange exchange, AttachmentContent attachment) {
    exchange
        .getConnection()
        .getWorker()
        .execute(
            () -> {
              TimeoutHolder timeoutHolder = attachment.getTimeoutHolder();

              if (timeoutHolder != null) {
                timeoutHolder.timeout();
                messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error(
                        "Timeout while sending to kafka message after "
                            + timeoutHolder.getTimeout()
                            + " milliseconds",
                        SENDING_TO_KAFKA_TIMEOUT));
              } else {
                messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error(
                        "Probably context switching problem as timeout task was started before it was attached to an exchange",
                        INTERNAL_ERROR));
              }
            });
  }
}
