package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentLoadProvider {

  private final Map<String, ConsumerNodeLoad> consumerLoads = new ConcurrentHashMap<>();
  private volatile SubscriptionProfiles profiles = SubscriptionProfiles.EMPTY;

  SubscriptionProfiles getProfiles() {
    return profiles;
  }

  ConsumerNodeLoad getConsumerNodeLoad(String consumerId) {
    return consumerLoads.getOrDefault(consumerId, ConsumerNodeLoad.UNDEFINED);
  }

  void updateConsumerNodeLoads(Map<String, ConsumerNodeLoad> newConsumerLoads) {
    consumerLoads.clear();
    consumerLoads.putAll(newConsumerLoads);
  }

  void updateProfiles(SubscriptionProfiles newProfiles) {
    profiles = newProfiles;
  }

  void clear() {
    consumerLoads.clear();
    profiles = SubscriptionProfiles.EMPTY;
  }
}
