package pl.allegro.tech.hermes.consumers.consumer.rate;

import pl.allegro.tech.hermes.api.Subscription;

public class BatchConsumerRateLimiter implements ConsumerRateLimiter {

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
