package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.TopicName;

public interface BrokerTopicManagement {

    void createTopic(TopicName name, RetentionTime retentionTime);

    void removeTopic(TopicName name);

    void updateTopic(TopicName name, RetentionTime retentionTime);

}
