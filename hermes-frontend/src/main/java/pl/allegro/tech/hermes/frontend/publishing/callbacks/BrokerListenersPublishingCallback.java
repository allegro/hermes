package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.MessageState;

public class BrokerListenersPublishingCallback implements PublishingCallback {

    private final BrokerListeners listeners;
    private final MessageState messageState;

    public BrokerListenersPublishingCallback(BrokerListeners listeners, MessageState messageState) {
        this.listeners = listeners;
        this.messageState = messageState;
    }

    @Override
    public void onUnpublished(Message message, Topic topic, Exception exception) {
        if (messageState.wasDelegatedToKafka()) {
            listeners.onError(message, topic, exception);
        }
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        listeners.onAcknowledge(message, topic);
    }
}
