package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecordersRegistry;

public interface ConsumerNodeLoadRegistry extends SubscriptionLoadRecordersRegistry {

  void start();

  void stop();

  ConsumerNodeLoad get(String consumerId);
}
