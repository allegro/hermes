package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.TopicName;

public interface SingleMessageReader {

    String readMessage(TopicName topicName, int partition, long offset);

}
