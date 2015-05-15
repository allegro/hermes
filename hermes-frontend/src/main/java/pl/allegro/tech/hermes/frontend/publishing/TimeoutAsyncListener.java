package pl.allegro.tech.hermes.frontend.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

import static pl.allegro.tech.hermes.frontend.publishing.MessageState.State.SENDING_TO_KAFKA;

class TimeoutAsyncListener implements AsyncListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutAsyncListener.class);

    private final HttpResponder httpResponder;
    private final MessageState messageState;

    TimeoutAsyncListener(HttpResponder httpResponder, MessageState messageState) {
        this.httpResponder = httpResponder;
        this.messageState = messageState;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        if (messageState.getState() != SENDING_TO_KAFKA) {
            httpResponder.timeout(event.getThrowable());
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
