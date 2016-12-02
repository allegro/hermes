package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface BrokerMessageProducer {

    void send(Message message, CachedTopic topic, PublishingCallback callback);

    boolean isTopicAvailable(CachedTopic topic);
}
