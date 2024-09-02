package pl.allegro.tech.hermes.management.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

public class MultiDCOffsetChangeSummary {

  private Map<String, List<PartitionOffset>> partitionOffsetListPerBrokerName = new HashMap<>();

  public MultiDCOffsetChangeSummary() {}

  @JsonAnySetter
  public void addPartitionOffsetList(
      String clusterName, List<PartitionOffset> partitionOffsetChange) {
    partitionOffsetListPerBrokerName.put(clusterName, partitionOffsetChange);
  }

  @JsonAnyGetter
  public Map<String, List<PartitionOffset>> getPartitionOffsetListPerBrokerName() {
    return ImmutableMap.copyOf(partitionOffsetListPerBrokerName);
  }
}
