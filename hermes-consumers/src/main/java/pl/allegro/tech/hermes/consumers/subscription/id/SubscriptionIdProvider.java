package pl.allegro.tech.hermes.consumers.subscription.id;

import pl.allegro.tech.hermes.api.SubscriptionName;

public interface SubscriptionIdProvider {

  SubscriptionId getSubscriptionId(SubscriptionName name);
}
