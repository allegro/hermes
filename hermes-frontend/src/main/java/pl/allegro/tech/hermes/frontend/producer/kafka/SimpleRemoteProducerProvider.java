package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

import java.util.Optional;

public class SimpleRemoteProducerProvider implements RemoteProducerProvider {

    @Override
    public Optional<KafkaProducer<byte[], byte[]>> get(CachedTopic cachedTopic, Producers producers) {
        var candidates = producers.getRemote(cachedTopic.getTopic());
        // TODO: check readiness
        if (candidates.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(candidates.get(0));

        }
    }

}
