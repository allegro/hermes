package pl.allegro.tech.hermes.management.domain.consistency;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class MetadataCopies {
  private final String id;
  private final Map<String, Object> copyPerDatacenter = new HashMap<>();

  MetadataCopies(String id, Set<String> datacenters) {
    this.id = id;
    datacenters.forEach(dc -> copyPerDatacenter.put(dc, null));
  }

  void put(String datacenter, Object value) {
    if (!copyPerDatacenter.containsKey(datacenter)) {
      throw new IllegalArgumentException("Invalid datacenter: " + datacenter);
    }
    copyPerDatacenter.put(datacenter, value);
  }

  boolean areAllEqual() {
    return copyPerDatacenter.values().stream().distinct().count() == 1;
  }

  String getId() {
    return id;
  }

  Map<String, Object> getCopyPerDatacenter() {
    return copyPerDatacenter;
  }
}
