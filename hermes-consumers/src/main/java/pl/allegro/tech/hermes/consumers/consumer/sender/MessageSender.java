package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.concurrent.CompletableFuture;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface MessageSender {

  CompletableFuture<MessageSendingResult> send(Message message);

  void stop();
}
