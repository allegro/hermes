package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.Topic;

public interface SingleMessageReader {

    String readMessage(Topic topic, int partition, long offset);

}
