package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

import java.time.Clock;
import java.util.Map;

public class KafkaConsumerRecordToMessageConverterFactory {

    private final MessageContentReaderFactory messageContentReaderFactory;
    private final Clock clock;

    public KafkaConsumerRecordToMessageConverterFactory(MessageContentReaderFactory messageContentReaderFactory, Clock clock) {
        this.messageContentReaderFactory = messageContentReaderFactory;
        this.clock = clock;
    }

    public KafkaConsumerRecordToMessageConverter create(Topic topic, Subscription subscription, Map<String, KafkaTopic> topics) {
        return new KafkaConsumerRecordToMessageConverter(topic, subscription, topics, messageContentReaderFactory.provide(topic), clock);
    }

}
