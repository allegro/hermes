package pl.allegro.tech.hermes.frontend.publishing;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;

import static pl.allegro.tech.hermes.frontend.publishing.MessageState.State.SENDING_TO_KAFKA;

public class BrokerTimeoutAsyncListener implements AsyncListener {

    private final HttpResponder httpResponder;
    private final Message message;
    private final Topic topic;
    private final MessageState messageState;
    private final BrokerListeners listeners;

    public BrokerTimeoutAsyncListener(HttpResponder httpResponder, Message message, Topic topic, MessageState messageState, BrokerListeners listeners) {
        this.httpResponder = httpResponder;
        this.message = message;
        this.topic = topic;
        this.messageState = messageState;
        this.listeners = listeners;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {

    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        if (messageState.getState() == SENDING_TO_KAFKA) {
            httpResponder.accept();
            listeners.onTimeout(message, topic);
        }
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {

    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {

    }
}
