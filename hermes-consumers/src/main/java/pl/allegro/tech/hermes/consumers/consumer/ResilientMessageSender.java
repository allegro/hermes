package pl.allegro.tech.hermes.consumers.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout;

public class ResilientMessageSender {
  private final ConsumerRateLimiter rateLimiter;
  private final List<Predicate<MessageSendingResult>> ignore;
  private final FutureAsyncTimeout async;
  private final int requestTimeoutMs;
  private final int asyncTimeoutMs;

  public ResilientMessageSender(
      ConsumerRateLimiter rateLimiter,
      Subscription subscription,
      FutureAsyncTimeout async,
      int requestTimeoutMs,
      int asyncTimeoutMs) {
    this.rateLimiter = rateLimiter;
    this.ignore = ignorableErrors(subscription);
    this.async = async;
    this.requestTimeoutMs = requestTimeoutMs;
    this.asyncTimeoutMs = asyncTimeoutMs;
  }

  private static List<Predicate<MessageSendingResult>> ignorableErrors(Subscription subscription) {
    Predicate<MessageSendingResult> ignore =
        result ->
            result.ignoreInRateCalculation(
                subscription.getSerialSubscriptionPolicy().isRetryClientErrors(),
                subscription.hasOAuthPolicy());
    return Collections.singletonList(ignore);
  }

  public <T extends MessageSendingResult> CompletableFuture<T> send(
      Consumer<CompletableFuture<T>> resultFutureConsumer, Function<Throwable, T> exceptionMapper) {
    try {
      CompletableFuture<T> resultFuture = new CompletableFuture<>();
      resultFutureConsumer.accept(resultFuture);
      CompletableFuture<T> timeoutGuardedResultFuture =
          async.within(
              resultFuture, Duration.ofMillis(asyncTimeoutMs + requestTimeoutMs), exceptionMapper);
      return withCompletionHandle(timeoutGuardedResultFuture, exceptionMapper);
    } catch (Exception e) {
      rateLimiter.registerFailedSending();
      return CompletableFuture.completedFuture(exceptionMapper.apply(e));
    }
  }

  private <T extends MessageSendingResult> CompletableFuture<T> withCompletionHandle(
      CompletableFuture<T> future, Function<Throwable, T> exceptionMapper) {
    return future.handle(
        (result, throwable) -> {
          if (throwable != null) {
            rateLimiter.registerFailedSending();
            return exceptionMapper.apply(throwable);
          } else {
            if (result.succeeded()) {
              rateLimiter.registerSuccessfulSending();
            } else {
              registerResultInRateLimiter(result);
            }
            return result;
          }
        });
  }

  private void registerResultInRateLimiter(MessageSendingResult result) {
    if (ignore.stream().anyMatch(p -> p.test(result))) {
      rateLimiter.registerSuccessfulSending();
    } else {
      rateLimiter.registerFailedSending();
    }
  }
}
