package pl.allegro.tech.hermes.consumers.subscription.id;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface SubscriptionIds {

  Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName);

  Optional<SubscriptionId> getSubscriptionId(long id);

  void start();
}
