package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SendFutureProvider;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SingleMessageSenderAdapter implements MessageSender {
    private final CompletableFutureAwareMessageSender adaptee;
    private final Function<Throwable, MessageSendingResult> exceptionMapper = MessageSendingResult::failedResult;

    private final SendFutureProvider sendFutureProvider;

    public SingleMessageSenderAdapter(CompletableFutureAwareMessageSender adaptee, SendFutureProvider sendFutureProvider) {
        this.sendFutureProvider = sendFutureProvider;
        this.adaptee = adaptee;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
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
