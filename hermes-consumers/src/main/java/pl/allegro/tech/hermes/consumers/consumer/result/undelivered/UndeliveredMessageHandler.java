package pl.allegro.tech.hermes.consumers.consumer.result.undelivered;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public interface UndeliveredMessageHandler {
    void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result);
}
