package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.concurrent.CompletableFuture;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.FieldMissingInDescriptorException;

public interface CompletableFutureAwareMessageSender {

  void send(Message message, CompletableFuture<MessageSendingResult> resultFuture);

  void stop();
}
