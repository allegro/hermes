package pl.allegro.tech.hermes.consumers.consumer.oauth;

import pl.allegro.tech.hermes.api.OAuthProvider;

public interface OAuthProviderCacheListener {

  void oAuthProviderUpdate(OAuthProvider oAuthProvider);
}
