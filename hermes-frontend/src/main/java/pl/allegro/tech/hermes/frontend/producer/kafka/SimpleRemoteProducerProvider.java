package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

import java.util.Optional;

public class SimpleRemoteProducerProvider implements RemoteProducerProvider {

    private final AdminReadinessService adminReadinessService;

    public SimpleRemoteProducerProvider(AdminReadinessService adminReadinessService) {
        this.adminReadinessService = adminReadinessService;
    }

    @Override
    public Optional<KafkaProducer<byte[], byte[]>> get(CachedTopic cachedTopic, Producers producers) {
        return producers.getRemote(cachedTopic.getTopic())
                .stream()
                .filter(producer -> adminReadinessService.isDatacenterReady(producer.getDatacenter()))
                .findFirst();
    }
}
