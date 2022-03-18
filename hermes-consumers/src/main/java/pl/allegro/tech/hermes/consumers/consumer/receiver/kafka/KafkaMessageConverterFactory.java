package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

import java.time.Clock;
import java.util.Map;

public class KafkaMessageConverterFactory {

    private final MessageContentReaderFactory messageContentReaderFactory;
    private final Clock clock;

    public KafkaMessageConverterFactory(MessageContentReaderFactory messageContentReaderFactory, Clock clock) {
        this.messageContentReaderFactory = messageContentReaderFactory;
        this.clock = clock;
    }

    public KafkaMessageConverter create(Topic topic, Subscription subscription, Map<String, KafkaTopic> topics) {
        return new KafkaMessageConverter(topic, subscription, topics, messageContentReaderFactory.provide(topic), clock);
    }

}
