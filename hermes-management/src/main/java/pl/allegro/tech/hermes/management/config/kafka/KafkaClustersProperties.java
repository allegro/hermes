package pl.allegro.tech.hermes.management.config.kafka;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kafka")
public class KafkaClustersProperties {
    private List<KafkaProperties> clusters = new ArrayList<>();

    public List<KafkaProperties> getClusters() {
        return clusters;
    }

    public void setClusters(List<KafkaProperties> clusters) {
        this.clusters = clusters;
    }
}
