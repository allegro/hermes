package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.RetentionTime;

public interface BrokerTopicManagement {

    void createTopic(TopicName name, int retentionPolicy);
    void removeTopic(TopicName name);
    void updateTopic(TopicName name, RetentionTime retentionTime);
}
