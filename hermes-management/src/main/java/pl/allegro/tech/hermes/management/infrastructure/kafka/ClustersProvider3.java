package pl.allegro.tech.hermes.management.infrastructure.kafka;

import java.util.List;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService;

public class ClustersProvider3 {
    private final List<BrokersClusterService> clusters;

    public ClustersProvider3(List<BrokersClusterService> clusters) {
        this.clusters = clusters;
    }

    public List<BrokersClusterService> getClusters() {
        return clusters;
    }
}
