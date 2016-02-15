package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;

public interface MessageBatchSenderFactory {
    MessageBatchSender create(Subscription subscription);
}
