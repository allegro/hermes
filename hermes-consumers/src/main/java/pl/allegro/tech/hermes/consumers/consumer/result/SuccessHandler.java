package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public interface SuccessHandler {
    void handle(Message message, Subscription subscription);
}
