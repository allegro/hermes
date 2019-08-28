package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId;

import java.util.Optional;

@FunctionalInterface
interface SubscriptionIdMapper {

    Optional<SubscriptionId> mapToSubscriptionId(SubscriptionName subscriptionName);
}
