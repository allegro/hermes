package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

/**
 * Interface for components that are aware of the subscription for which the message is being
 * processed. This can be used to implement subscription-specific logic in message processing, such
 * as filtering messages, modifying results based on subscription properties, or applying different
 * processing strategies for different subscriptions.
 */
public interface SubscriptionAware {
  default boolean appliesTo(Subscription subscription) {
    return true;
  }
}
