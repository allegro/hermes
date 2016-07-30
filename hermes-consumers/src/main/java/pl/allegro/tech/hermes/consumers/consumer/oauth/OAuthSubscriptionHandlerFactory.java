package pl.allegro.tech.hermes.consumers.consumer.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import javax.inject.Inject;
import java.util.Optional;

public class OAuthSubscriptionHandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(OAuthSubscriptionHandlerFactory.class);

    private final SubscriptionRepository subscriptionRepository;

    private final OAuthAccessTokens accessTokens;

    private final OAuthTokenRequestRateLimiterFactory rateLimiterLoader;

    @Inject
    public OAuthSubscriptionHandlerFactory(SubscriptionRepository subscriptionRepository,
                                           OAuthAccessTokens accessTokens,
                                           OAuthTokenRequestRateLimiterFactory rateLimiterLoader) {
        this.subscriptionRepository = subscriptionRepository;
        this.accessTokens = accessTokens;
        this.rateLimiterLoader = rateLimiterLoader;
    }

    public Optional<OAuthSubscriptionHandler> create(SubscriptionName subscriptionName) {
        Subscription subscription = subscriptionRepository.getSubscriptionDetails(subscriptionName);
        if (subscription.hasOAuthPolicy()) {
            try {
                String providerName = subscription.getOAuthPolicy().getProviderName();
                logger.info("Creating OAuth handler subscription {} using {} OAuth provider",
                        subscriptionName, providerName);
                OAuthTokenRequestRateLimiter rateLimiter = rateLimiterLoader.create(subscription);
                return Optional.of(new OAuthSubscriptionHandler(subscriptionName, providerName, accessTokens, rateLimiter));
            } catch (Exception e) {
                logger.error("Failed to create OAuth handler for subscription {}, {}",
                        subscriptionName.getQualifiedName(), e.getMessage());
            }
        }
        return Optional.empty();
    }
}
