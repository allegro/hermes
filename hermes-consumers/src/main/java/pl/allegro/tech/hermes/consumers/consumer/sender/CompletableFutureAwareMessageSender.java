package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.concurrent.CompletableFuture;

public interface CompletableFutureAwareMessageSender {

    void send(Message message, CompletableFuture<MessageSendingResult> resultFuture);

    void stop();
}
