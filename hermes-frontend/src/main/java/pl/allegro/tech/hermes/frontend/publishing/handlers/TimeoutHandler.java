package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageEndProcessor;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
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
            delayedSending(exchange, attachment);
        }
    }

    private void delayedSending(HttpServerExchange exchange, AttachmentContent attachment) {
        messageEndProcessor.bufferedButDelayed(exchange, attachment);
    }

    private void readingTimeout(HttpServerExchange exchange, AttachmentContent attachment) {
        TimeoutHolder timeoutHolder = attachment.getTimeoutHolder();
        timeoutHolder.timeout();

        ErrorDescription error = error("Timeout while reading message after milliseconds: " + timeoutHolder.getTimeout(), TIMEOUT);

        messageErrorProcessor.sendQuietly(exchange, error, attachment.getMessageId(), attachment.getTopic().getQualifiedName());
        // switch logging from io to worker thread as it can be blocking operation
        exchange.getConnection().getWorker().execute(() ->
                messageErrorProcessor.log(error, attachment.getTopic(), attachment.getMessageId(), exchange.getHostAndPort()));
    }
}
