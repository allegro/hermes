package pl.allegro.tech.hermes.domain.oauth;

import pl.allegro.tech.hermes.api.OAuthProvider;

import java.util.List;

public interface OAuthProviderRepository {

    boolean oAuthProviderExists(String oAuthProviderName);

    void ensureOAuthProviderExists(String oAuthProviderName);

    List<String> listOAuthProviderNames();

    List<OAuthProvider> listOAuthProviders();

    OAuthProvider getOAuthProviderDetails(String oAuthProviderName);

    void createOAuthProvider(OAuthProvider oAuthProvider);

    void updateOAuthProvider(OAuthProvider oAuthprovider);

    void removeOAuthProvider(String oAuthProviderName);
}
