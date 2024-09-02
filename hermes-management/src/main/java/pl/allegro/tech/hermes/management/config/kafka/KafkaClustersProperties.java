package pl.allegro.tech.hermes.management.config.kafka;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public class KafkaClustersProperties {
  private List<KafkaProperties> clusters = new ArrayList<>();
  private String defaultNamespace = "";
  private String namespaceSeparator = "_";

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

  public String getNamespaceSeparator() {
    return namespaceSeparator;
  }

  public void setNamespaceSeparator(String namespaceSeparator) {
    this.namespaceSeparator = namespaceSeparator;
  }
}
