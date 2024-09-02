package pl.allegro.tech.hermes.consumers.consumer.load;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface SubscriptionLoadRecordersRegistry {

  SubscriptionLoadRecorder register(SubscriptionName subscriptionName);
}
