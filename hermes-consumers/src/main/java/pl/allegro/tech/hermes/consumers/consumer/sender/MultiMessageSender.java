package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SendFutureProvider;

import java.util.concurrent.CompletableFuture;

public abstract class MultiMessageSender implements MessageSender {

    private final SendFutureProvider sendFutureProvider;

    public MultiMessageSender(SendFutureProvider sendFutureProvider) {
        this.sendFutureProvider = sendFutureProvider;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        return sendMany(message, sendFutureProvider);
    }

    protected abstract CompletableFuture<MessageSendingResult> sendMany(Message message, SendFutureProvider sendFutureProvider);


    @Override
    public void stop() {
    }
}
