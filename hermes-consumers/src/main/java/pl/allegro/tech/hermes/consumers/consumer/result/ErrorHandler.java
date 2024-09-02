package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public interface ErrorHandler {
  void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result);

  void handleFailed(Message message, Subscription subscription, MessageSendingResult result);
}
