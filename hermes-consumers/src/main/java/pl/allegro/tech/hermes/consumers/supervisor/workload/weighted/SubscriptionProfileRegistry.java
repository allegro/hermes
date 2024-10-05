package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

public interface SubscriptionProfileRegistry {

  SubscriptionProfiles fetch();

  void persist(SubscriptionProfiles profiles);
}
