package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;
import static pl.allegro.tech.hermes.frontend.publishing.handlers.ContentLengthChecker.checkContentLength;

class MessageReadHandler implements HttpHandler {

    private final HttpHandler next;
    private final HttpHandler timeoutHandler;
    private final MessageErrorProcessor messageErrorProcessor;
    private final int defaultAsyncTimeout;
    private final int longAsyncTimeout;

    MessageReadHandler(HttpHandler next, HttpHandler timeoutHandler, ConfigFactory configFactory,
                              MessageErrorProcessor messageErrorProcessor) {
        this.next = next;
        this.timeoutHandler = timeoutHandler;
        this.messageErrorProcessor = messageErrorProcessor;
        this.defaultAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_IDLE_TIMEOUT);
        this.longAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_LONG_IDLE_TIMEOUT);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);

        int timeout = attachment.getTopic().isReplicationConfirmRequired() ? longAsyncTimeout : defaultAsyncTimeout;

        attachment.setTimeoutHolder(new TimeoutHolder(
                timeout,
                exchange.getIoThread().executeAfter(
                    () -> exchange.dispatch(timeoutHandler),
                    timeout,
                    MILLISECONDS)));

        readMessage(exchange, attachment);
    }

    private void readMessage(HttpServerExchange exchange, AttachmentContent attachment) {
        ByteArrayOutputStream messageContent = new ByteArrayOutputStream();

        StartedTimersPair startedTimersPair = attachment.getTopicWithMetrics().startRequestReadTimers();

        attachment.getTimeoutHolder().onTimeout((Void) -> {
            startedTimersPair.close();
            exchange.getRequestReceiver().pause();
        });

        MessageState state = attachment.getMessageState();

        exchange.getRequestReceiver().receivePartialBytes(
                (exchange1, message, last) -> {
                    state.onNotTimeout((Void) -> messageContent.write(message, 0, message.length));
                    if (last) {
                        state.onFullyReadSet((Void) -> {
                            startedTimersPair.close();
                            messageRead(exchange1, messageContent.toByteArray(), attachment);
                        });
                    }
                },
                (exchange1, e) -> {
                    startedTimersPair.close();
                    readException(exchange, attachment, e);
                });
    }

    private void messageRead(HttpServerExchange exchange, byte[] messageContent, AttachmentContent attachment) {
        try {
            checkContentLength(exchange, messageContent.length);
            attachment.setMessageContent(messageContent);
            next.handleRequest(exchange);
        } catch (ContentLengthChecker.InvalidContentLengthException e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(),
                    attachment.getMessageId(), error(e.getMessage(), VALIDATION_ERROR));
        } catch (Exception e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(), e);
        }
    }

    public void readException(HttpServerExchange exchange, AttachmentContent attachment, IOException exception) {
        ErrorDescription error = error("Error while reading message. " + getRootCauseMessage(exception), INTERNAL_ERROR);
        if (exchange.getConnection().isOpen()) {
            messageErrorProcessor.sendQuietly(exchange, error, attachment.getMessageId());
        }
        messageErrorProcessor.log(
                error, attachment.getTopicWithMetrics().getTopic(), attachment.getMessageId(), exchange.getHostAndPort());
    }
}
