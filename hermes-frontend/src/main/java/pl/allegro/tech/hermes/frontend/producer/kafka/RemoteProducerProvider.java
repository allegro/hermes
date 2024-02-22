package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Optional;

public interface RemoteProducerProvider {
    Optional<Producer<byte[], byte[]>> get(CachedTopic cachedTopic, Producers producers);
}
