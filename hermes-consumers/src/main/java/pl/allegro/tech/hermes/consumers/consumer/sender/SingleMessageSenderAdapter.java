package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SingleMessageSenderAdapter implements MessageSender {
    private final CompletableFutureAwareMessageSender adaptee;
    private final Function<Throwable, MessageSendingResult> exceptionMapper = MessageSendingResult::failedResult;

    public SingleMessageSenderAdapter(CompletableFutureAwareMessageSender adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message, SendFutureProvider sendFutureProvider) {
        return sendFutureProvider.provide(
                resultFuture -> adaptee.send(message, resultFuture),
                exceptionMapper
        );
    }

    @Override
    public void stop() {
        adaptee.stop();
    }
}
