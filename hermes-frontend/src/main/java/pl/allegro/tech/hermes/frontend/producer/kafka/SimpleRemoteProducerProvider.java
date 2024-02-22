package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.Producer;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

import java.util.Map;
import java.util.Optional;

public class SimpleRemoteProducerProvider implements RemoteProducerProvider {

    private final AdminReadinessService adminReadinessService;

    public SimpleRemoteProducerProvider(AdminReadinessService adminReadinessService) {
        this.adminReadinessService = adminReadinessService;
    }

    @Override
    public Optional<Producer<byte[], byte[]>> get(CachedTopic cachedTopic, Producers producers) {
        var candidates = producers.getRemote(cachedTopic.getTopic());
        return candidates.entrySet().stream()
                .filter(e -> {
                    var datacenter = e.getKey();
                    return adminReadinessService.isDatacenterReady(datacenter);
                })
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
