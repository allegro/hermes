package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.google.common.base.Preconditions;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;

public class OAuthTokenRequestRateLimiterFactory {

  private final OAuthProviderRepository oAuthProviderRepository;

  private final double rateReductionFactor;

  public OAuthTokenRequestRateLimiterFactory(
      OAuthProviderRepository oAuthProviderRepository, double rateReductionFactor) {
    this.oAuthProviderRepository = oAuthProviderRepository;
    this.rateReductionFactor = rateReductionFactor;

    Preconditions.checkArgument(
        rateReductionFactor >= 1,
        "Token request rate limiter rate reduction factor must be greater or equal to 1");
  }

  public OAuthTokenRequestRateLimiter create(Subscription subscription) {
    String providerName = subscription.getOAuthPolicy().getProviderName();
    OAuthProvider oAuthProvider = oAuthProviderRepository.getOAuthProviderDetails(providerName);
    double initialRate = delayToRate(oAuthProvider.getTokenRequestInitialDelay());
    double minimalRate = delayToRate(oAuthProvider.getTokenRequestMaxDelay());
    return new OAuthTokenRequestRateLimiter(
        initialRate, minimalRate, rateReductionFactor, oAuthProvider.getTokenRequestInitialDelay());
  }

  private double delayToRate(Integer delay) {
    return 1000.0 / delay;
  }
}
