package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.RateLimitingMessageSender;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SingleRecipientMessageSenderAdapter implements MessageSender {
    private final CompletableFutureAwareMessageSender adaptee;
    private final Function<Throwable, MessageSendingResult> exceptionMapper = MessageSendingResult::failedResult;

    private final RateLimitingMessageSender rateLimitingMessageSender;

    public SingleRecipientMessageSenderAdapter(CompletableFutureAwareMessageSender adaptee, RateLimitingMessageSender rateLimitingMessageSender) {
        this.rateLimitingMessageSender = rateLimitingMessageSender;
        this.adaptee = adaptee;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        return rateLimitingMessageSender.send(
                resultFuture -> adaptee.send(message, resultFuture),
                exceptionMapper
        );
    }

    @Override
    public void stop() {
        adaptee.stop();
    }
}
