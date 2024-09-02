package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OAuthTokenRequestRateLimiter {

  private final double initialRate;

  private final double minimalRate;

  private final double rateReductionFactor;

  private final RateLimiter rateLimiter;

  private final AtomicInteger rateReductionsCount = new AtomicInteger();

  public OAuthTokenRequestRateLimiter(
      double initialRate, double minimalRate, double rateReductionFactor, long warmUpPeriod) {
    this.initialRate = initialRate;
    this.rateLimiter = RateLimiter.create(initialRate, warmUpPeriod, TimeUnit.MILLISECONDS);
    this.minimalRate = minimalRate;
    this.rateReductionFactor = rateReductionFactor;
  }

  public boolean tryAcquire() {
    return rateLimiter.tryAcquire();
  }

  public void reduceRate() {
    rateReductionsCount.incrementAndGet();
    rateLimiter.setRate(Math.max(rateLimiter.getRate() / rateReductionFactor, minimalRate));
  }

  public void resetRate() {
    if (rateReductionsCount.get() > 0) {
      rateLimiter.setRate(initialRate);
      rateReductionsCount.set(0);
    }
  }

  public double getCurrentRate() {
    return rateLimiter.getRate();
  }
}
