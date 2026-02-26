package pl.allegro.tech.hermes.benchmark.consumer;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter;

public class NoOpConsumerRateLimiter implements ConsumerRateLimiter {
  @Override
  public void initialize() {}

  @Override
  public void shutdown() {}

  @Override
  public void acquire() {}

  @Override
  public void acquireFiltered() {}

  @Override
  public void adjustConsumerRate() {}

  @Override
  public void updateSubscription(Subscription newSubscription) {}

  @Override
  public void registerSuccessfulSending() {}

  @Override
  public void registerFailedSending() {}
}
