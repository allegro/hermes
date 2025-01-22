package pl.allegro.tech.hermes.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

class HermesClientTermination {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final long pollInterval;

  HermesClientTermination(long pollInterval) {
    this.pollInterval = pollInterval;
  }

  CompletableFuture<Void> observe(BooleanSupplier condition) {
    final CompletableFuture<Void> result = new CompletableFuture<>();

    executorService.submit(
        () -> {
          try {
            while (!condition.getAsBoolean()) {
              Thread.sleep(pollInterval);
            }
            result.complete(null);
          } catch (InterruptedException e) {
            result.completeExceptionally(e);
            Thread.currentThread().interrupt();
          } finally {
            executorService.shutdown();
          }
        });

    return result;
  }
}
