package pl.allegro.tech.hermes.consumers.consumer.sender;

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
            return CompletableFuture.completedFuture(MessageSendingResult.failedResult(e));
        }
    }

    protected abstract void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture);

}
