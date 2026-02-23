package pl.allegro.tech.hermes.management.config.zookeeper;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@ConfigurationProperties(prefix = "management.zookeeper")
public class ZookeeperClustersProperties {

  private boolean transactional = true;

  private List<ZookeeperProperties> clusters = new ArrayList<>();

  public boolean isTransactional() {
    return transactional;
  }

  public void setTransactional(boolean transactional) {
    this.transactional = transactional;
  }

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
