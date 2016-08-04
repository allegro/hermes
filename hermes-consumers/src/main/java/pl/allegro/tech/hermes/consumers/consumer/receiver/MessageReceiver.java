package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.Optional;

public interface MessageReceiver {

    Optional<Message> next();

    default void stop() {}

    default void update(Subscription newSubscription) {}
}
