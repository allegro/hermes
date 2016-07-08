package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import static pl.allegro.tech.hermes.api.ErrorCode.TIMEOUT;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

class TimeoutHandler implements HttpHandler {

    private final MessageErrorProcessor messageErrorProcessor;
    private final MessageEndProcessor messageEndProcessor;

    TimeoutHandler(MessageEndProcessor messageEndProcessor, MessageErrorProcessor messageErrorProcessor) {
        this.messageErrorProcessor = messageErrorProcessor;
        this.messageEndProcessor = messageEndProcessor;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
        MessageState state = attachment.getMessageState();

        state.setTimeoutHasPassed();
        if (state.setReadingTimeout()) {
            readingTimeout(exchange, attachment);
        } else if (state.setDelayedSending()) {
            delayedSending(exchange, attachment.getTopic(), attachment.getMessage());
        }
    }

    private void delayedSending(HttpServerExchange exchange, Topic topic, Message message) {
        exchange.getConnection().getWorker().execute(() ->
                messageEndProcessor.bufferedButDelayed(exchange, topic, message));
    }

    private void readingTimeout(HttpServerExchange exchange, AttachmentContent attachment) {
        exchange.getConnection().getWorker().execute(() -> {
            TimeoutHolder timeoutHolder = attachment.getTimeoutHolder();
            timeoutHolder.timeout();

            messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error("Timeout while reading message after milliseconds: " + timeoutHolder.getTimeout(), TIMEOUT));
                });
    }
}
