package pl.allegro.tech.hermes.frontend.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;
import pl.allegro.tech.hermes.frontend.publishing.message.RequestTimeoutLock;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

class TimeoutAsyncListener implements AsyncListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutAsyncListener.class);

    private final HttpResponder httpResponder;
    private final MessageState messageState;
    private final RequestTimeoutLock requestTimeoutLock;

    TimeoutAsyncListener(HttpResponder httpResponder, MessageState messageState, RequestTimeoutLock requestTimeoutLock) {
        this.httpResponder = httpResponder;
        this.messageState = messageState;
        this.requestTimeoutLock = requestTimeoutLock;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        requestTimeoutLock.lock();
        if (messageState.equals(MessageState.State.WAITING_FOR_FIRST_PORTION_OF_DATA) ||
                messageState.equals(MessageState.State.PARSING) ||
                messageState.equals(MessageState.State.PARSED)) {
            httpResponder.timeout(event.getThrowable());
        } else if (messageState.equals(MessageState.State.SENDING_TO_KAFKA)) {
            httpResponder.accept();
        }

    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        LOGGER.error("This error should not occur", event.getThrowable());
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
}
