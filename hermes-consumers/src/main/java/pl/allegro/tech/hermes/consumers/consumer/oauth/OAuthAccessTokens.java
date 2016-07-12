package pl.allegro.tech.hermes.consumers.consumer.oauth;

import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.Optional;

public interface OAuthAccessTokens {

    Optional<OAuthAccessToken> loadToken(SubscriptionName subscription);

    Optional<OAuthAccessToken> getTokenIfPresent(SubscriptionName subscription);

    void refreshToken(SubscriptionName subscription);

    boolean tokenExists(SubscriptionName subscription);
}
