package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;

public interface Support {
    default boolean supports(Subscription subscription) {
        return true;
    }
}
