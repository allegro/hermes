package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import java.time.Clock;
import java.util.Map;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

public class KafkaConsumerRecordToMessageConverterFactory {

  private final MessageContentReaderFactory messageContentReaderFactory;
  private final KafkaHeaderExtractor kafkaHeaderExtractor;
  private final Clock clock;

  public KafkaConsumerRecordToMessageConverterFactory(
      MessageContentReaderFactory messageContentReaderFactory,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      Clock clock) {
    this.messageContentReaderFactory = messageContentReaderFactory;
    this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    this.clock = clock;
  }

  public KafkaConsumerRecordToMessageConverter create(
      Topic topic, Subscription subscription, Map<String, KafkaTopic> topics) {
    return new KafkaConsumerRecordToMessageConverter(
        topic,
        subscription,
        topics,
        messageContentReaderFactory.provide(topic),
        kafkaHeaderExtractor,
        clock);
  }
}
