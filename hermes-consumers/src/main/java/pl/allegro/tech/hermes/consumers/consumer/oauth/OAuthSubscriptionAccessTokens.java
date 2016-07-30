package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.Optional;

public class OAuthSubscriptionAccessTokens implements OAuthAccessTokens {

    private static final Logger logger = LoggerFactory.getLogger(OAuthSubscriptionAccessTokens.class);

    private final LoadingCache<SubscriptionName, OAuthAccessToken> subscriptionTokens;

    private final OAuthAccessTokensLoader tokenLoader;

    @Inject
    public OAuthSubscriptionAccessTokens(OAuthAccessTokensLoader tokenLoader,
                                         ConfigFactory configFactory) {
        this.tokenLoader = tokenLoader;
        this.subscriptionTokens = CacheBuilder.newBuilder()
                .maximumSize(configFactory.getLongProperty(Configs.OAUTH_SUBSCRIPTION_TOKENS_CACHE_MAX_SIZE))
                .build(tokenLoader);
    }

    @Override
    public Optional<OAuthAccessToken> getTokenIfPresent(SubscriptionName subscriptionName) {
        return Optional.ofNullable(subscriptionTokens.getIfPresent(subscriptionName));
    }

    @Override
    public Optional<OAuthAccessToken> loadToken(SubscriptionName subscriptionName) {
        try {
            return Optional.ofNullable(subscriptionTokens.get(subscriptionName));
        } catch (Exception e) {
            logger.error("Could not get access token for subscription {}", subscriptionName, e);
            return Optional.empty();
        }
    }

    @Override
    public void refreshToken(SubscriptionName subscriptionName) {
        try {
            OAuthAccessToken token = tokenLoader.load(subscriptionName);
            subscriptionTokens.put(subscriptionName, token);
        } catch (Exception e) {
            logger.error("An error occurred while refreshing access token for subscription {}",
                    subscriptionName, e);
        }
    }

    @Override
    public boolean tokenExists(SubscriptionName subscriptionName) {
        return subscriptionTokens.getIfPresent(subscriptionName) != null;
    }
}
