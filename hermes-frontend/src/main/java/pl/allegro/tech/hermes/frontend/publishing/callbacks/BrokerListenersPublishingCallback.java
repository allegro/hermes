package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class BrokerListenersPublishingCallback implements PublishingCallback {

    private final BrokerListeners listeners;

    public BrokerListenersPublishingCallback(BrokerListeners listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onUnpublished(Message message, Topic topic, Exception exception) {
        listeners.onError(message, topic, exception);
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        listeners.onAcknowledge(message, topic);
    }
}
