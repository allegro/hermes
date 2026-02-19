package pl.allegro.tech.hermes.benchmark.consumer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class InMemoryDelayedMessageSender implements MessageSender {
  private final LongAdder longAdder = new LongAdder();
  private final ExecutorService executorService = Executors.newFixedThreadPool(4);
  private final Executor executor =
      CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS, executorService);

  @Override
  public void stop() {}

  @Override
  public CompletableFuture<MessageSendingResult> send(Message message) {
    return CompletableFuture.supplyAsync(
        () -> {
          longAdder.increment();
          return MessageSendingResult.succeededResult();
        },
        executor);
  }

  public long getSentMessagesCount() {
    return longAdder.sum();
  }

  public void shutdown() {
    executorService.shutdown();
  }

  public void reset() {
    longAdder.reset();
  }
}
