package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.concurrent.CompletableFuture;

public abstract class CompletableFutureAwareMessageSender implements MessageSender {

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        try {
            CompletableFuture<MessageSendingResult> resultFuture = new CompletableFuture<>();
            sendMessage(message, resultFuture);
            return resultFuture;
        } catch (Exception e) {
            throw new InternalProcessingException("Failed to send message", e);
        }
    }

    protected abstract void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture);

}
