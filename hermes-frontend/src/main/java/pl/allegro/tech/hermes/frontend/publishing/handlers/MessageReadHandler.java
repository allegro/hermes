package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.io.Receiver;
import io.undertow.server.DefaultResponseListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.MessageErrorProcessor;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.THROUGHPUT_QUOTA_VIOLATION;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.api.ErrorDescription.error;

class MessageReadHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageReadHandler.class);

    private final HttpHandler next;
    private final HttpHandler timeoutHandler;
    private final MessageErrorProcessor messageErrorProcessor;
    private final ContentLengthChecker contentLengthChecker;
    private final int defaultAsyncTimeout;
    private final int longAsyncTimeout;
    private final ThroughputLimiter throughputLimiter;

    MessageReadHandler(HttpHandler next, HttpHandler timeoutHandler, ConfigFactory configFactory,
                       MessageErrorProcessor messageErrorProcessor,  ThroughputLimiter throughputLimiter) {
        this.next = next;
        this.timeoutHandler = timeoutHandler;
        this.messageErrorProcessor = messageErrorProcessor;
        this.contentLengthChecker = new ContentLengthChecker(configFactory);
        this.defaultAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_IDLE_TIMEOUT);
        this.longAsyncTimeout = configFactory.getIntProperty(Configs.FRONTEND_LONG_IDLE_TIMEOUT);
        this.throughputLimiter = throughputLimiter;
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

        ThroughputLimiter.QuotaInsight quotaInsight = throughputLimiter.checkQuota(
                attachment.getCachedTopic().getTopicName(),
                attachment.getCachedTopic().getThroughput());
        if (quotaInsight.hasQuota()) {
            readMessage(exchange, attachment);
        } else {
            respondWithQuotaViolation(exchange, attachment, quotaInsight.getReason());
        }
    }

    private void runTimeoutHandler(HttpServerExchange exchange, AttachmentContent attachment) {
        try {
            timeoutHandler.handleRequest(exchange);
        } catch (Exception e) {
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                    error("Error while handling timeout task", INTERNAL_ERROR), e);
        }
    }

    private void readMessage(HttpServerExchange exchange, AttachmentContent attachment) {
        ByteArrayOutputStream messageContent = new ByteArrayOutputStream();
        MessageState state = attachment.getMessageState();

        StartedTimersPair readingTimers = attachment.getCachedTopic().startRequestReadTimers();

        Receiver receiver = exchange.getRequestReceiver();

        attachment.getTimeoutHolder().onTimeout((Void) -> {
            readingTimers.close();
            receiver.pause();
        });

        if (state.setReading()) {
            receiver.receivePartialBytes(
                    partialMessageRead(state, messageContent, readingTimers, attachment),
                    readingError(state, readingTimers, attachment));
        } else {
            readingTimers.close();
            messageErrorProcessor.sendAndLog(
                    exchange,
                    attachment.getTopic(),
                    attachment.getMessageId(),
                    error("Probably context switching problem as timeout elapsed before message reading was started", INTERNAL_ERROR));
        }
    }

    private Receiver.PartialBytesCallback partialMessageRead(MessageState state, ByteArrayOutputStream messageContent,
                                                             StartedTimersPair readingTimers, AttachmentContent attachment) {
        return (exchange, message, last) -> {
            if (state.isReadingTimeout()) {
                endWithoutDefaultResponse(exchange);
                return;
            }
            messageContent.write(message, 0, message.length);

            if (last) {
                if (state.setFullyRead()) {
                    readingTimers.close();
                    messageRead(exchange, messageContent.toByteArray(), attachment);
                } else {
                    endWithoutDefaultResponse(exchange);
                }
            }
        };
    }

    private Receiver.ErrorCallback readingError(MessageState state, StartedTimersPair readingTimers, AttachmentContent attachment) {
        return (exchange, exception) -> {
            if (state.setReadingError()) {
                readingTimers.close();
                attachment.removeTimeout();
                messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                        error("Error while reading message. " + getRootCauseMessage(exception), INTERNAL_ERROR), exception);
            } else {
                messageErrorProcessor.log(
                        exchange,
                        "Error while reading message after timeout execution. " + getRootCauseMessage(exception),
                        exception);
            }
        };
    }

    private void messageRead(HttpServerExchange exchange, byte[] messageContent, AttachmentContent attachment) {
        try {
            contentLengthChecker.check(exchange, messageContent.length, attachment);
            attachment.getCachedTopic().reportMessageContentSize(messageContent.length);
            ThroughputLimiter.QuotaInsight quotaCheck = throughputLimiter.checkQuota(
                    attachment.getCachedTopic().getTopicName(),
                    attachment.getCachedTopic().getThroughput());
            if (quotaCheck.hasQuota()) {
                finalizeMessageRead(exchange, messageContent, attachment);
            } else {
                respondWithQuotaViolation(exchange, attachment, quotaCheck.getReason());
            }
        } catch (ContentLengthChecker.InvalidContentLengthException | ContentLengthChecker.ContentTooLargeException e) {
            attachment.removeTimeout();
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(),
                    attachment.getMessageId(), error(e.getMessage(), VALIDATION_ERROR));
        } catch (Exception e) {
            attachment.removeTimeout();
            messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(), e);
        }
    }

    private void finalizeMessageRead(HttpServerExchange exchange,
                                     byte[] messageContent,
                                     AttachmentContent attachment) throws Exception {
        attachment.setMessageContent(messageContent);
        endWithoutDefaultResponse(exchange);
        if (exchange.isInIoThread()) {
            dispatchToWorker(exchange, attachment);
        } else {
            next.handleRequest(exchange);
        }
    }

    private void respondWithQuotaViolation(HttpServerExchange exchange,
                                           AttachmentContent attachment,
                                           String reason) {
        attachment.removeTimeout();
        messageErrorProcessor.sendAndLog(
                exchange,
                attachment.getTopic(),
                attachment.getMessageId(),
                error(reason, THROUGHPUT_QUOTA_VIOLATION));
    }

    private void dispatchToWorker(HttpServerExchange exchange, AttachmentContent attachment) {
        // exchange.dispatch(next) is not called here because async io read flag can be still set to true which combined with
        // dispatch() leads to an exception
        exchange.getConnection().getWorker().execute(() -> {
            try {
                next.handleRequest(exchange);
            } catch (Exception e) {
                attachment.removeTimeout();
                messageErrorProcessor.sendAndLog(exchange, attachment.getTopic(), attachment.getMessageId(),
                        error("Error while executing next handler after read handler", INTERNAL_ERROR), e);
            }
        });
    }

    private void endWithoutDefaultResponse(HttpServerExchange exchange) {
        // when a handler doesn't return a response (for example when is interrupted by timeout)
        // then without this listener default response can be returned with 200 status code when the handler finishes
        // execution before the other one
        exchange.addDefaultResponseListener(new DefaultResponseSimulator());
    }

    final static class DefaultResponseSimulator implements DefaultResponseListener {

        private static final boolean RESPONSE_SIMULATED = true;
        private final AtomicBoolean responseNotSimulatedOnlyOnce = new AtomicBoolean();

        @Override
        public boolean handleDefaultResponse(HttpServerExchange exchange) {
            if (exchange.getAttachment(AttachmentContent.KEY).isResponseReady()) {
                if (exchange.getStatusCode() == 200) {
                    try {
                        exchange.setStatusCode(500);
                    } catch (RuntimeException e) {
                        logger.error("Exception has been thrown during an exchange status modification", e);
                    }
                }
                return !responseNotSimulatedOnlyOnce.compareAndSet(false, true);
            } else {
                return RESPONSE_SIMULATED;
            }
        }
    }
}
