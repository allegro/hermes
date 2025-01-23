package pl.allegro.tech.hermes.consumers.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@ConfigurationProperties(prefix = "consumer.zookeeper")
public class ZookeeperClustersProperties {

  private List<ZookeeperProperties> clusters = new ArrayList<>();

  public List<ZookeeperProperties> getClusters() {
    return clusters;
  }

  public void setClusters(List<ZookeeperProperties> clusters) {
    this.clusters = clusters;
  }

  public ZookeeperProperties toZookeeperProperties(DatacenterNameProvider datacenterNameProvider) {
    return this.clusters.stream()
        .filter(
            cluster -> cluster.getDatacenter().equals(datacenterNameProvider.getDatacenterName()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No properties for datacenter: "
                        + datacenterNameProvider.getDatacenterName()
                        + " defined."));
  }
}
