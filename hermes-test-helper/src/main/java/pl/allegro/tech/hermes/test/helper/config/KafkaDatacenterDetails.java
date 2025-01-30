package pl.allegro.tech.hermes.test.helper.config;

import java.util.List;

public class KafkaDatacenterDetails {

  private final String datacenter;
  private final List<String> remoteDatacenters;

  public KafkaDatacenterDetails(String datacenter) {
    this.datacenter = datacenter;
    this.remoteDatacenters = List.of();
  }

  public KafkaDatacenterDetails(String datacenter, List<String> remoteDatacenters) {
    this.datacenter = datacenter;
    this.remoteDatacenters = remoteDatacenters;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public List<String> getRemoteDatacenters() {
    return remoteDatacenters;
  }
}
