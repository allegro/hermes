package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;

@FunctionalInterface
interface SubscriptionIdMapper {

  Optional<SubscriptionId> mapToSubscriptionId(SubscriptionName subscriptionName);
}
