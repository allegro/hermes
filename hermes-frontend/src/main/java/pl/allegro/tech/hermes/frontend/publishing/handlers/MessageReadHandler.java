package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

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
                        () -> runTimeoutHandler(exchange, attachment),
                        timeout,
                        MILLISECONDS)));

        readMessage(exchange, attachment);
    }

    private void runTimeoutHandler(HttpServerExchange exchange, AttachmentContent attachment) {
        try {
            timeoutHandler.handleRequest(exchange);
        } catch (Exception e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                    error("Error while handling timeout task.", INTERNAL_ERROR), e);
        }
    }

    private void readMessage(HttpServerExchange exchange, AttachmentContent attachment) {
        ByteArrayOutputStream messageContent = new ByteArrayOutputStream();

        StartedTimersPair startedTimersPair = attachment.getCachedTopic().startRequestReadTimers();

        attachment.getTimeoutHolder().onTimeout((Void) -> {
            startedTimersPair.close();
            exchange.getRequestReceiver().pause();
        });

        MessageState state = attachment.getMessageState();

        exchange.getRequestReceiver().receivePartialBytes(
                (exchange1, message, last) -> {
                    if (state.isReadingTimeout()) {
                        endWithoutDefaultResponse(exchange);
                        return;
                    }
                    messageContent.write(message, 0, message.length);

                    if (last) {
                        if (state.setFullyRead()) {
                            startedTimersPair.close();
                            messageRead(exchange1, messageContent.toByteArray(), attachment);
                        } else {
                            endWithoutDefaultResponse(exchange);
                        }
                    }
                },
                (exchange1, e) -> {
                    startedTimersPair.close();
                    messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                            error("Error while reading message. " + getRootCauseMessage(e), INTERNAL_ERROR), e);
                });
    }

    private void messageRead(HttpServerExchange exchange, byte[] messageContent, AttachmentContent attachment) {
        try {
            checkContentLength(exchange, messageContent.length);
            attachment.getCachedTopic().reportMessageContentSize(messageContent.length);
            attachment.setMessageContent(messageContent);
            if (exchange.isInIoThread()) {
                dispatchToWorker(exchange, attachment);
            } else {
                next.handleRequest(exchange);
            }
        } catch (ContentLengthChecker.InvalidContentLengthException e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(),
                    attachment.getMessageId(), error(e.getMessage(), VALIDATION_ERROR));
        } catch (Exception e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(), e);
        }
    }

    private void dispatchToWorker(HttpServerExchange exchange, AttachmentContent attachment) {
        // exchange.dispatch(next) is not called here because async io read flag can be still set to true which combined with
        // dispatch() leads to an exception
        exchange.getConnection().getWorker().execute(() -> {
            try {
                next.handleRequest(exchange);
            } catch (Exception e) {
                messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                        error("Error while executing handler next to read handler.", INTERNAL_ERROR), e);
            }
        });
        endWithoutDefaultResponse(exchange);
    }

    private void endWithoutDefaultResponse(HttpServerExchange exchange) {
        // when a handler doesn't return a response (for example when is interrupted by timeout)
        // then without this listener default response can be returned with 200 status code when the handler finishes
        // execution before the other one
        exchange.addDefaultResponseListener(new ResponseListener());
    }

    private final static class ResponseListener implements DefaultResponseListener {

        private static final Logger logger = LoggerFactory.getLogger(ResponseListener.class);

        private static final boolean END_WITHOUT_RESPONSE = true;
        private final AtomicBoolean returnResponseOnlyOnce = new AtomicBoolean();

        @Override
        public boolean handleDefaultResponse(HttpServerExchange exchange) {
            if (exchange.getAttachment(EXCEPTION) != null) {
                logger.error("Exception caught while running response listener", exchange.getAttachment(EXCEPTION));
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    return false;
                }
            }

            if (exchange.getAttachment(AttachmentContent.KEY).isResponseReady()) {
                return !returnResponseOnlyOnce.compareAndSet(false, true);
            } else {
                return END_WITHOUT_RESPONSE;
            }
        }
    }
}
