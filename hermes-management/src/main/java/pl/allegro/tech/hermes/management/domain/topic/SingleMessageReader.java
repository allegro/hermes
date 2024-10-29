package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

public interface SingleMessageReader {

  String readMessageAsJson(Topic topic, KafkaTopic kafkaTopic, int partition, long offset);
}
