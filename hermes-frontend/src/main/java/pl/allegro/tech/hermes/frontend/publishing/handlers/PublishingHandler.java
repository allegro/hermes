package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

class PublishingHandler implements HttpHandler {

    private final BrokerMessageProducer brokerMessageProducer;
    private final MessageErrorProcessor messageErrorProcessor;
    private final MessageEndProcessor messageEndProcessor;

    PublishingHandler(BrokerMessageProducer brokerMessageProducer, MessageErrorProcessor messageErrorProcessor,
                      MessageEndProcessor messageEndProcessor) {
        this.brokerMessageProducer = brokerMessageProducer;
        this.messageErrorProcessor = messageErrorProcessor;
        this.messageEndProcessor = messageEndProcessor;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // change state of exchange to dispatched,
        // thanks to this call, default response with 200 status code is not returned after handlerRequest() finishes its execution
        exchange.dispatch(() -> {
            try {
                handle(exchange);
            } catch (RuntimeException e) {
                messageErrorProcessor.sendAndLog(exchange, "Exception while publishing message to a broker.", e);
            }
        });
    }

    private void handle(HttpServerExchange exchange) {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
        MessageState messageState = attachment.getMessageState();

        messageState.setSendingToKafkaProducerQueue();
        StartedTimersPair brokerLatencyTimers = attachment.getCachedTopic().startBrokerLatencyTimers();
        brokerMessageProducer.send(attachment.getMessage(), attachment.getCachedTopic(), new PublishingCallback() {

            // called from kafka producer thread
            @Override
            public void onPublished(Message message, Topic topic) {
                exchange.getConnection().getWorker().execute(() -> {
                    brokerLatencyTimers.close();
                    if (messageState.setSentToKafka()) {
                        attachment.removeTimeout();
                        messageEndProcessor.sent(exchange, attachment);
                    } else if (messageState.setDelayedSentToKafka()) {
                        messageEndProcessor.delayedSent(exchange, attachment.getCachedTopic(), message);
                    }
                });
            }

            // in most cases this method should be called from worker thread,
            // therefore there is no need to switch it to another worker thread
            @Override
            public void onUnpublished(Message message, Topic topic, Exception exception) {
                messageState.setErrorInSendingToKafka();
                brokerLatencyTimers.close();
                attachment.removeTimeout();
                handleNotPublishedMessage(exchange, topic, attachment.getMessageId(), exception);
            }
        });

        if (messageState.setSendingToKafka() && messageState.setDelayedProcessing()) {
            messageEndProcessor.bufferedButDelayedProcessing(exchange, attachment);
        }
    }

    private void handleNotPublishedMessage(HttpServerExchange exchange, Topic topic, String messageId, Exception exception) {
        messageErrorProcessor.sendAndLog(
                exchange,
                topic,
                messageId,
                error("Message not published. " + getRootCauseMessage(exception), INTERNAL_ERROR));
    }
}
