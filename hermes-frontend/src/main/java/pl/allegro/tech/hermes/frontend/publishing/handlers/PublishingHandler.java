package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

class PublishingHandler implements HttpHandler {

  private final BrokerMessageProducer brokerMessageProducer;
  private final MessageErrorProcessor messageErrorProcessor;
  private final MessageEndProcessor messageEndProcessor;

  PublishingHandler(
      BrokerMessageProducer brokerMessageProducer,
      MessageErrorProcessor messageErrorProcessor,
      MessageEndProcessor messageEndProcessor) {
    this.brokerMessageProducer = brokerMessageProducer;
    this.messageErrorProcessor = messageErrorProcessor;
    this.messageEndProcessor = messageEndProcessor;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    // change state of exchange to dispatched,
    // thanks to this call, default response with 200 status code is not returned after
    // handlerRequest() finishes its execution
    exchange.dispatch(
        () -> {
          try {
            handle(exchange);
          } catch (RuntimeException e) {
            AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
            MessageState messageState = attachment.getMessageState();
            messageState.setErrorInSendingToKafka();
            messageErrorProcessor.sendAndLog(
                exchange, "Exception while publishing message to a broker.", e);
          }
        });
  }

  private void handle(HttpServerExchange exchange) {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
    MessageState messageState = attachment.getMessageState();

    messageState.setSendingToKafkaProducerQueue();
    brokerMessageProducer.send(
        attachment.getMessage(),
        attachment.getCachedTopic(),
        new PublishingCallback() {

          @Override
          public void onPublished(Message message, Topic topic) {
            exchange
                .getConnection()
                .getWorker()
                .execute(
                    () -> {
                      if (messageState.setSentToKafka()) {
                        attachment.removeTimeout();
                        messageEndProcessor.sent(exchange, attachment);
                      } else if (messageState.setDelayedSentToKafka()) {
                        messageEndProcessor.delayedSent(attachment.getCachedTopic(), message);
                      }
                    });
          }

          @Override
          public void onEachPublished(Message message, Topic topic, String datacenter) {
            exchange
                .getConnection()
                .getWorker()
                .execute(
                    () -> {
                      attachment.getCachedTopic().incrementPublished(datacenter);
                      messageEndProcessor.eachSent(exchange, attachment, datacenter);
                    });
          }

          @Override
          public void onUnpublished(Message message, Topic topic, Exception exception) {
            exchange
                .getConnection()
                .getWorker()
                .execute(
                    () -> {
                      messageState.setErrorInSendingToKafka();
                      attachment.removeTimeout();
                      handleNotPublishedMessage(
                          exchange, topic, attachment.getMessageId(), exception);
                    });
          }
        });

    if (messageState.setSendingToKafka()
        && !attachment.getCachedTopic().getTopic().isFallbackToRemoteDatacenterEnabled()
        && messageState.setDelayedProcessing()) {
      messageEndProcessor.bufferedButDelayedProcessing(exchange, attachment);
    }
  }

  private void handleNotPublishedMessage(
      HttpServerExchange exchange, Topic topic, String messageId, Exception exception) {
    messageErrorProcessor.sendAndLog(
        exchange,
        topic,
        messageId,
        error("Message not published. " + getRootCauseMessage(exception), INTERNAL_ERROR));
  }
}
