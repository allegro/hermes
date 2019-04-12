package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.codahale.metrics.Timer;
import com.google.common.cache.CacheLoader;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthTokenRequest;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.GrantType.USERNAME_PASSWORD;
import static pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthTokenRequest.oAuthTokenRequest;

public class OAuthAccessTokensLoader extends CacheLoader<SubscriptionName, OAuthAccessToken> {

    private final SubscriptionRepository subscriptionRepository;

    private final OAuthProviderRepository oAuthProviderRepository;

    private final OAuthClient oAuthClient;

    private final HermesMetrics metrics;

    @Inject
    public OAuthAccessTokensLoader(SubscriptionRepository subscriptionRepository,
                                   OAuthProviderRepository oAuthProviderRepository,
                                   OAuthClient oAuthClient,
                                   HermesMetrics metrics) {
        this.subscriptionRepository = subscriptionRepository;
        this.oAuthProviderRepository = oAuthProviderRepository;
        this.oAuthClient = oAuthClient;
        this.metrics = metrics;
    }

    @Override
    public OAuthAccessToken load(SubscriptionName subscriptionName) throws Exception {
        Subscription subscription = subscriptionRepository.getSubscriptionDetails(subscriptionName);
        SubscriptionOAuthPolicy oAuthPolicy = subscription.getOAuthPolicy();
        String providerName = oAuthPolicy.getProviderName();
        OAuthProvider oAuthProvider = oAuthProviderRepository.getOAuthProviderDetails(providerName);
        OAuthTokenRequest request;
        if (USERNAME_PASSWORD.equals(oAuthPolicy.getGrantType())) {
            request = getOAuthUsernamePasswordGrantTokenRequest(oAuthPolicy, oAuthProvider);
        } else {
            request = getOAuthClientCredentialsGrantTokenRequest(oAuthPolicy, oAuthProvider);
        }

        metrics.oAuthSubscriptionTokenRequestMeter(subscription, providerName).mark();
        try (Timer.Context timer = metrics.oAuthProviderLatencyTimer(providerName).time()) {
            return oAuthClient.getToken(request);
        }
    }

    private OAuthTokenRequest getOAuthUsernamePasswordGrantTokenRequest(SubscriptionOAuthPolicy policy, OAuthProvider provider) {
        return oAuthTokenRequest()
                .withUrl(provider.getTokenEndpoint())
                .withGrantType(OAuthTokenRequest.GrantTypeValue.RESOURCE_OWNER_USERNAME_PASSWORD)
                .withScope(policy.getScope())
                .withClientId(provider.getClientId())
                .withClientSecret(provider.getClientSecret())
                .withUsername(policy.getUsername())
                .withPassword(policy.getPassword())
                .withRequestTimeout(provider.getRequestTimeout())
                .withSocketTimeout(provider.getSocketTimeout())
                .build();
    }

    private OAuthTokenRequest getOAuthClientCredentialsGrantTokenRequest(SubscriptionOAuthPolicy policy, OAuthProvider provider) {
        return oAuthTokenRequest()
                .withUrl(provider.getTokenEndpoint())
                .withGrantType(OAuthTokenRequest.GrantTypeValue.CLIENT_CREDENTIALS)
                .withScope(policy.getScope())
                .withClientId(provider.getClientId())
                .withClientSecret(provider.getClientSecret())
                .withRequestTimeout(provider.getRequestTimeout())
                .withSocketTimeout(provider.getSocketTimeout())
                .build();
    }
}
