package pl.allegro.tech.hermes.management.infrastructure.kafka;

import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;

import java.util.List;

public class ClustersProvider {
    private final List<BrokersClusterService> clusters;

    public ClustersProvider(List<BrokersClusterService> clusters) {
        this.clusters = clusters;
    }

    public List<BrokersClusterService> getClusters() {
        return clusters;
    }
}
