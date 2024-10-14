package pl.allegro.tech.hermes.consumers.consumer.oauth;

import java.util.Optional;
import pl.allegro.tech.hermes.api.SubscriptionName;

public interface OAuthAccessTokens {

  Optional<OAuthAccessToken> loadToken(SubscriptionName subscription);

  Optional<OAuthAccessToken> getTokenIfPresent(SubscriptionName subscription);

  void refreshToken(SubscriptionName subscription);

  boolean tokenExists(SubscriptionName subscription);
}
