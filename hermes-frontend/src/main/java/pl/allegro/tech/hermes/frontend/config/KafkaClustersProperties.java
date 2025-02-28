package pl.allegro.tech.hermes.frontend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

@ConfigurationProperties(prefix = "frontend.kafka")
public class KafkaClustersProperties {

  Logger logger = LoggerFactory.getLogger(KafkaClustersProperties.class);

  private List<KafkaProperties> clusters = new ArrayList<>();

  private String namespace = "";

  private String namespaceSeparator = "_";

  public List<KafkaProperties> getClusters() {
    return clusters;
  }

  public void setClusters(List<KafkaProperties> clusters) {
    this.clusters = clusters;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getNamespaceSeparator() {
    return namespaceSeparator;
  }

  public void setNamespaceSeparator(String namespaceSeparator) {
    this.namespaceSeparator = namespaceSeparator;
  }

  public KafkaProperties toKafkaProperties(DatacenterNameProvider datacenterNameProvider) {
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

  public List<KafkaParameters> toRemoteKafkaProperties(
      DatacenterNameProvider datacenterNameProvider) {
    String currentDatacenterName = datacenterNameProvider.getDatacenterName();
    List<String> remoteDatacenters =
        this.clusters.stream()
            .filter(cluster -> cluster.getDatacenter().equals(currentDatacenterName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No properties for datacenter: " + currentDatacenterName + " defined."))
            .getRemoteDatacenters();

    List<KafkaParameters> filteredClusters =
        this.clusters.stream()
            .filter(
                cluster ->
                    remoteDatacenters.contains(cluster.getDatacenter())
                        && !cluster.getDatacenter().equals(currentDatacenterName))
            .collect(Collectors.toList());

    List<String> filteredRemoteDatacenters =
        filteredClusters.stream().map(KafkaParameters::getDatacenter).collect(Collectors.toList());

    logger.info("Remote datacenters for {}: {}", currentDatacenterName, filteredRemoteDatacenters);

    return filteredClusters;
  }
}
