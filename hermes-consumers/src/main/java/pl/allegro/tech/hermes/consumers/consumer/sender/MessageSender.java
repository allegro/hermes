package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.util.concurrent.CompletableFuture;

public interface MessageSender {

    CompletableFuture<MessageSendingResult> send(Message message);

    void stop();
}
