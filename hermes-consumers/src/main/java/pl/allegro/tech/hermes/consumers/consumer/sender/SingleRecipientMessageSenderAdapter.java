package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;

public class SingleRecipientMessageSenderAdapter implements MessageSender {
  private final CompletableFutureAwareMessageSender adaptee;
  private final Function<Throwable, MessageSendingResult> exceptionMapper =
      MessageSendingResult::failedResult;

  private final ResilientMessageSender resilientMessageSender;

  public SingleRecipientMessageSenderAdapter(
      CompletableFutureAwareMessageSender adaptee, ResilientMessageSender resilientMessageSender) {
    this.resilientMessageSender = resilientMessageSender;
    this.adaptee = adaptee;
  }

  @Override
  public CompletableFuture<MessageSendingResult> send(Message message) {
    return resilientMessageSender.send(
        resultFuture -> adaptee.send(message, resultFuture), exceptionMapper);
  }

  @Override
  public void stop() {
    adaptee.stop();
  }
}
