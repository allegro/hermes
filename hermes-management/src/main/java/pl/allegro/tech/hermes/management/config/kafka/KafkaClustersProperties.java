package pl.allegro.tech.hermes.management.config.kafka;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kafka")
public class KafkaClustersProperties {
    private List<KafkaProperties> clusters = new ArrayList<>();
    private String defaultNamespace = "";

    public List<KafkaProperties> getClusters() {
        return clusters;
    }

    public void setClusters(List<KafkaProperties> clusters) {
        this.clusters = clusters;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }
}
