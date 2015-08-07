package pl.allegro.tech.hermes.frontend.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

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
        if (!messageState.wasDelegatedToKafka()) {
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
