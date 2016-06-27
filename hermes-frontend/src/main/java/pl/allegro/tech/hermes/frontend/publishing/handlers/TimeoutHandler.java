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

        if (state.onDelayedSendingSet((Void) -> delayedSending(exchange, attachment.getTopic(), attachment.getMessage()))) {
            return;
        } else if (state.onReadingTimeoutSet((Void) -> readingTimeout(exchange, attachment))) {
            return;
        } else {
            state.setDelayedProcessing();
        }
    }

    private void delayedSending(HttpServerExchange exchange, Topic topic, Message message) {
        messageEndProcessor.bufferedButDelayed(exchange, topic, message);
    }

    private void readingTimeout(HttpServerExchange exchange, AttachmentContent attachment) {
        TimeoutHolder timeoutHolder = attachment.getTimeoutHolder();
        timeoutHolder.timeout();
        messageErrorProcessor.sendAndLog(
                exchange,
                attachment.getTopic(),
                attachment.getMessageId(),
                error("Timeout while reading message after milliseconds: " + timeoutHolder.getTimeout(), TIMEOUT));
    }
}
