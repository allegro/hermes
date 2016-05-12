package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public interface BrokerMessageProducer {

    void send(Message message, Topic topic, PublishingCallback callback);
    boolean isTopicAvailable(Topic topic);

}
