package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jetty.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.ConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthAccessTokensLoader;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthConsumerAuthorizationHandler;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthProvidersNotifyingCache;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionAccessTokens;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthSubscriptionHandlerFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.OAuthTokenRequestRateLimiterFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthHttpClient;
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


@Configuration
public class OAuthConfiguration {

    @Bean
    public OAuthTokenRequestRateLimiterFactory oAuthTokenRequestRateLimiterFactory(OAuthProviderRepository oAuthProviderRepository,
                                                                                   ConfigFactory configFactory) {
        return new OAuthTokenRequestRateLimiterFactory(oAuthProviderRepository, configFactory);
    }

    @Bean
    public OAuthAccessTokens oAuthSubscriptionAccessTokens(OAuthAccessTokensLoader tokenLoader,
                                                           ConfigFactory configFactory) {
        return new OAuthSubscriptionAccessTokens(tokenLoader, configFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public OAuthClient oAuthHttpClient(@Named("oauth-http-client") HttpClient httpClient,
                                       ObjectMapper objectMapper) {
        return new OAuthHttpClient(httpClient, objectMapper);
    }

    @Bean
    public OAuthSubscriptionHandlerFactory oAuthSubscriptionHandlerFactory(SubscriptionRepository subscriptionRepository,
                                                                           OAuthAccessTokens accessTokens,
                                                                           OAuthTokenRequestRateLimiterFactory rateLimiterLoader) {
        return new OAuthSubscriptionHandlerFactory(subscriptionRepository, accessTokens, rateLimiterLoader);
    }

    @Bean
    public OAuthAccessTokensLoader oAuthAccessTokensLoader(SubscriptionRepository subscriptionRepository,
                                                           OAuthProviderRepository oAuthProviderRepository,
                                                           OAuthClient oAuthClient,
                                                           HermesMetrics metrics) {
        return new OAuthAccessTokensLoader(subscriptionRepository, oAuthProviderRepository, oAuthClient, metrics);
    }

    @Bean
    public OAuthProvidersNotifyingCache oAuthProvidersNotifyingCache(CuratorFramework curator,
                                                                     ZookeeperPaths paths,
                                                                     ObjectMapper objectMapper) {
        String path = paths.oAuthProvidersPath();
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oauth-providers-notifying-cache-%d").build();
        ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
        OAuthProvidersNotifyingCache cache = new OAuthProvidersNotifyingCache(curator, path, executorService, objectMapper);
        try {
            cache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start Zookeeper cache for path " + path, e);
        }
        return cache;
    }

    @Bean
    public ConsumerAuthorizationHandler oAuthConsumerAuthorizationHandler(OAuthSubscriptionHandlerFactory handlerFactory,
                                                                          ConfigFactory configFactory,
                                                                          OAuthProvidersNotifyingCache oAuthProvidersCache) {
        return new OAuthConsumerAuthorizationHandler(handlerFactory, configFactory, oAuthProvidersCache);
    }
}
