package pl.allegro.tech.hermes.frontend.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;
import pl.allegro.tech.hermes.tracker.frontend.Trackers;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static pl.allegro.tech.hermes.api.ErrorCode.INTERNAL_ERROR;
import static pl.allegro.tech.hermes.api.ErrorCode.TIMEOUT;
import static pl.allegro.tech.hermes.api.ErrorCode.VALIDATION_ERROR;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

public class HttpResponder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponder.class);

    private Trackers trackers;
    private String messageId;
    private HttpServletResponse response;
    private AsyncContext asyncContext;
    private Topic topic;
    private ErrorSender errorSender;
    private MessageState messageState;
    private String remoteHost;
    private boolean completed = false;

    public HttpResponder(
            Trackers trackers,
            String messageId,
            HttpServletResponse response,
            AsyncContext asyncContext,
            Topic topic,
            ErrorSender errorSender,
            MessageState messageState,
            String remoteHost) {

        this.trackers = trackers;
        this.messageId = messageId;
        this.response = response;
        this.asyncContext = asyncContext;
        this.topic = topic;
        this.errorSender = errorSender;
        this.messageState = messageState;
        this.remoteHost = remoteHost;
    }

    public void accept() {
        trackers.get(topic).logInflight(messageId, topic.getName());
        completeCorrect(SC_ACCEPTED);
    }

    public void ok() {
        trackers.get(topic).logPublished(messageId, topic.getName());
        completeCorrect(SC_CREATED);
    }

    public void timeout(Throwable throwable) {
        completeError(new ErrorDescription(formatErrorMessage("Async timeout", throwable), TIMEOUT));
    }

    public void badRequest(Throwable throwable, String message) {
        completeError(new ErrorDescription(formatErrorMessage(message, throwable), VALIDATION_ERROR));
    }

    public void badRequest(Throwable throwable) {
        completeError(new ErrorDescription(throwable.getMessage(), VALIDATION_ERROR));
    }

    public void internalError(Throwable throwable, String message) {
        completeError(new ErrorDescription(formatErrorMessage(message, throwable), INTERNAL_ERROR));
    }

    private void completeError(ErrorDescription desc) {
        synchronized (this) {
            if (completed) {
                LOGGER.warn("Response already sent. Error message {}, topic {}, remote host {}, message state {}",
                        desc.getMessage(), topic.getName().qualifiedName(), remoteHost, messageState.getState().name()
                );
                return;
            }
            completed = true;

            LOGGER.error(
                    "{}, publishing on topic {}, remote host {}, message state {}",
                    desc.getMessage(), topic.getName().qualifiedName(), remoteHost, messageState.getState().name()
            );

            errorSender.sendErrorResponseQuietly(desc, response, messageId);
            asyncContext.complete();
        }

        trackers.get(topic).logError(messageId, topic.getName(), desc.getMessage());
    }

    private void completeCorrect(int status) {
        synchronized (this) {
            if (completed) {
                return;
            }
            completed = true;

            response.setStatus(status);
            response.setHeader(MESSAGE_ID.getName(), messageId);
            asyncContext.complete();
        }
    }

    private String formatErrorMessage(String message, Throwable throwable) {
        return format("%s, cause: %s", message, throwable == null ? "unknown" : throwable.getMessage());
    }
}
