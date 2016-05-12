package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.api.OAuth2AuthenticationData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.cache.zookeeper.NodeCache;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import com.github.scribejava.core.builder.api.DefaultApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.model.OAuthConfig;

public class OAuth2AuthProvider implements HttpAuthorizationProvider {

    private final OAuth2AuthenticationData authData;

    private final OAuth20Service service;

    private String accessToken;

    public OAuth2AuthProvider(Subscription subscription) {
        this.authData = subscription.getOAuth2AuthenticationData();

        service = new ServiceBuilder()
                .apiKey(authData.getConsumerKey())
                .apiSecret(authData.getConsumerSecret())
                .build(new OAuthApi(authData.getAccessTokenEndpoint()));
        obtainAccessToken();
    }

    @Override
    public String authorizationToken(Message message) {
        if (message.getNumberOfUnauthorizedRequests() == 1) {
            obtainAccessToken();
        }
        return accessToken;
    }

    public String obtainAccessToken() {
        OAuth2AccessToken token = service.getAccessTokenPasswordGrant(authData.getUsername(), authData.getPassword());
        accessToken = "Bearer " + token.getAccessToken();
        return accessToken;
    }

    private class OAuthApi extends DefaultApi20 {
        private final String accessTokenEndpoint;

        public OAuthApi(String accessTokenEndpoint) {
            this.accessTokenEndpoint = accessTokenEndpoint;
        }

        @Override
        public String getAccessTokenEndpoint() {
            return accessTokenEndpoint;
        }

        @Override
        public String getAuthorizationUrl(OAuthConfig config) {
            return null;
        }
    }
}
