package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MessageSenderAdapter implements MessageSender {
    private final CompletableFutureAwareMessageSender adaptee;
    private final SendFutureProvider<MessageSendingResult> futureProvider;
    private final Function<Throwable, MessageSendingResult> exceptionMapper = MessageSendingResult::failedResult;

    public MessageSenderAdapter(CompletableFutureAwareMessageSender adaptee, SendFutureProvider<MessageSendingResult> futureProvider) {
        this.adaptee = adaptee;
        this.futureProvider = futureProvider;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        return futureProvider.provide(
                resultFuture -> adaptee.send(message, resultFuture),
                exceptionMapper
        );
    }

    @Override
    public void stop() {
        adaptee.stop();
    }
}
