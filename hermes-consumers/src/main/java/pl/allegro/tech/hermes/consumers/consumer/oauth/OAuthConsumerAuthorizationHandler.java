package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class OAuthConsumerAuthorizationHandler
    implements ConsumerAuthorizationHandler, OAuthProviderCacheListener {

  private static final Logger logger =
      LoggerFactory.getLogger(OAuthConsumerAuthorizationHandler.class);

  private final OAuthSubscriptionHandlerFactory handlerFactory;

  private final Map<SubscriptionName, OAuthSubscriptionHandler> handlers =
      new ConcurrentHashMap<>();

  private final RateLimiter missingHandlersCreationRateLimiter;

  public OAuthConsumerAuthorizationHandler(
      OAuthSubscriptionHandlerFactory handlerFactory,
      Duration missingSubscriptionHandlersCreationDelay,
      OAuthProvidersNotifyingCache oAuthProvidersCache) {
    this.handlerFactory = handlerFactory;
    this.missingHandlersCreationRateLimiter =
        RateLimiter.create(missingSubscriptionHandlersCreationDelay.toSeconds());
    oAuthProvidersCache.setListener(this);
  }

  @Override
  public void createSubscriptionHandler(SubscriptionName subscriptionName) {
    handlerFactory
        .create(subscriptionName)
        .ifPresent(
            handler -> {
              logger.info("OAuth handler for subscription {} created", subscriptionName);
              handlers.put(subscriptionName, handler);
              handler.initialize();
            });
  }

  @Override
  public void removeSubscriptionHandler(SubscriptionName subscriptionName) {
    if (handlers.remove(subscriptionName) != null) {
      logger.info("OAuth handler for subscription {} removed", subscriptionName);
    }
  }

  @Override
  public void updateSubscription(SubscriptionName subscriptionName) {
    removeSubscriptionHandler(subscriptionName);
    createSubscriptionHandler(subscriptionName);
  }

  @Override
  public void oAuthProviderUpdate(OAuthProvider oAuthProvider) {
    logger.info("Updated OAuth provider {}", oAuthProvider.getName());
    List<SubscriptionName> subscriptions =
        handlers.entrySet().stream()
            .filter(entry -> entry.getValue().getProviderName().equals(oAuthProvider.getName()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    subscriptions.forEach(this::updateSubscription);
  }

  @Override
  public void handleSuccess(
      Message message, Subscription subscription, MessageSendingResult result) {
    if (shouldHandle(subscription)) {
      getSubscriptionHandler(subscription.getQualifiedName())
          .ifPresent(OAuthSubscriptionHandler::handleSuccess);
    }
  }

  @Override
  public void handleFailed(
      Message message, Subscription subscription, MessageSendingResult result) {
    if (shouldHandle(subscription)) {
      getSubscriptionHandler(subscription.getQualifiedName())
          .ifPresent(h -> h.handleFailed(subscription, result));
    }
  }

  @Override
  public void handleDiscarded(
      Message message, Subscription subscription, MessageSendingResult result) {}

  private boolean shouldHandle(Subscription subscription) {
    return subscription.hasOAuthPolicy();
  }

  private Optional<OAuthSubscriptionHandler> getSubscriptionHandler(
      SubscriptionName subscriptionName) {
    if (handlers.containsKey(subscriptionName)) {
      return Optional.of(handlers.get(subscriptionName));
    }
    return tryCreatingMissingHandler(subscriptionName);
  }

  private Optional<OAuthSubscriptionHandler> tryCreatingMissingHandler(
      SubscriptionName subscriptionName) {
    if (missingHandlersCreationRateLimiter.tryAcquire()) {
      createSubscriptionHandler(subscriptionName);
      return Optional.ofNullable(handlers.get(subscriptionName));
    }
    return Optional.empty();
  }
}
