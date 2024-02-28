package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Optional;

public interface RemoteProducerProvider {
    Optional<KafkaProducer<byte[], byte[]>> get(CachedTopic cachedTopic, Producers producers);
}
