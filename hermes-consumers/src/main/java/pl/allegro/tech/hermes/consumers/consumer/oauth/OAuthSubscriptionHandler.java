package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class OAuthSubscriptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(OAuthSubscriptionHandler.class);

  private final SubscriptionName subscriptionName;

  private final String providerName;

  private final OAuthAccessTokens accessTokens;

  private final OAuthTokenRequestRateLimiter rateLimiter;

  private final ScheduledExecutorService executorService;

  public OAuthSubscriptionHandler(
      SubscriptionName subscriptionName,
      String providerName,
      OAuthAccessTokens accessTokens,
      OAuthTokenRequestRateLimiter rateLimiter) {
    this.subscriptionName = subscriptionName;
    this.providerName = providerName;
    this.accessTokens = accessTokens;
    this.rateLimiter = rateLimiter;
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat(subscriptionName.getQualifiedName() + "-oauth-handler-%d")
            .build();
    this.executorService = Executors.newScheduledThreadPool(1, threadFactory);
  }

  public void initialize() {
    rateLimiter.tryAcquire();
    accessTokens.loadToken(subscriptionName);
  }

  public String getProviderName() {
    return providerName;
  }

  public void handleSuccess() {
    rateLimiter.resetRate();
  }

  public void handleFailed(Subscription subscription, MessageSendingResult result) {
    SubscriptionName subscriptionName = subscription.getQualifiedName();
    if (shouldTryRefreshingToken(subscriptionName, result)) {
      if (rateLimiter.tryAcquire()) {
        logger.info("Refreshing token for subscription {}", subscriptionName);
        rateLimiter.reduceRate();
        executorService.schedule(
            () -> accessTokens.refreshToken(subscriptionName), 0, TimeUnit.MILLISECONDS);
      }
    }
  }

  private boolean shouldTryRefreshingToken(
      SubscriptionName subscriptionName, MessageSendingResult result) {
    return result.getStatusCode() == HttpStatus.UNAUTHORIZED_401
        || !accessTokens.tokenExists(subscriptionName);
  }
}
