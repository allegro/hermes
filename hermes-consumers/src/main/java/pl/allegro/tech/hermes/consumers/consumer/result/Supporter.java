package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface Supporter {
  default boolean supports(Subscription subscription) {
    return true;
  }
}
