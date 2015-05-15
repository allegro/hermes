package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.publishing.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class BrokerListenersPublishingCallback implements PublishingCallback {

    private final BrokerListeners listeners;

    public BrokerListenersPublishingCallback(BrokerListeners listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onUnpublished(Exception exception) {

    }

    @Override
    public void onPublished(Message message, Topic topic) {
        listeners.onAcknowledge(message, topic);
    }
}
