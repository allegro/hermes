package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProviderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.AuthHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HermesHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http1HeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.Http2HeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Optional.empty;

public class JettyHttpMessageSenderProvider implements ProtocolMessageSenderProvider {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpMessageSenderProvider.class);

    private static final HttpHeadersProvider http1HeadersProvider = new Http1HeadersProvider();
    private static final HttpHeadersProvider http2HeadersProvider = new Http2HeadersProvider();

    private final HttpClient httpClient;
    private final Optional<HttpClient> http2Client;
    private final EndpointAddressResolver endpointAddressResolver;
    private final MetadataAppender<Request> metadataAppender;
    private final HttpAuthorizationProviderFactory authorizationProviderFactory;

    @Inject
    public JettyHttpMessageSenderProvider(
            ConfigFactory config,
            HttpClientFactory httpClientFactory,
            EndpointAddressResolver endpointAddressResolver,
            MetadataAppender<Request> metadataAppender,
            HttpAuthorizationProviderFactory authorizationProviderFactory) {
        this.httpClient = httpClientFactory.provide();
        this.http2Client = config.getBooleanProperty(Configs.CONSUMER_HTTP2_ENABLED) ?
                Optional.of(httpClientFactory.createClientForHttp2()) : empty();
        this.endpointAddressResolver = endpointAddressResolver;
        this.metadataAppender = metadataAppender;
        this.authorizationProviderFactory = authorizationProviderFactory;
    }

    @Override
    public MessageSender create(Subscription subscription) {
        EndpointAddress endpoint = subscription.getEndpoint();
        EndpointAddressResolverMetadata endpointAddressResolverMetadata = subscription.getEndpointAddressResolverMetadata();
        ResolvableEndpointAddress resolvableEndpoint = new ResolvableEndpointAddress(endpoint,
                endpointAddressResolver, endpointAddressResolverMetadata);
        HttpRequestFactory requestFactory = httpRequestFactory(subscription);

        if (subscription.getMode() == SubscriptionMode.BROADCAST) {
            return new JettyBroadCastMessageSender(requestFactory, resolvableEndpoint);
        } else {
            return new JettyMessageSender(requestFactory, resolvableEndpoint);
        }
    }

    private HttpRequestFactory httpRequestFactory(Subscription subscription) {
        int requestTimeout = subscription.getSerialSubscriptionPolicy().getRequestTimeout();
        int socketTimeout = subscription.getSerialSubscriptionPolicy().getSocketTimeout();

        return new HttpRequestFactory(
                getHttpClient(subscription),
                requestTimeout,
                socketTimeout,
                metadataAppender,
                getHttpRequestHeadersProvider(subscription)
        );
    }

    private HttpHeadersProvider getHttpRequestHeadersProvider(Subscription subscription) {
        Optional<HttpAuthorizationProvider> authorizationProvider = authorizationProviderFactory.create(subscription);
        HttpHeadersProvider httpHeadersProvider = subscription.isHttp2Enabled() ? http2HeadersProvider : http1HeadersProvider;

        return new HermesHeadersProvider(
                new AuthHeadersProvider(
                        httpHeadersProvider,
                        authorizationProvider.orElse(null)
                )
        );
    }

    private HttpClient getHttpClient(Subscription subscription) {
        if (subscription.isHttp2Enabled()) {
            return tryToGetHttp2Client(subscription);
        } else {
            logger.info("Using http/1.1 for {}.", subscription.getQualifiedName());
            return httpClient;
        }
    }

    private HttpClient tryToGetHttp2Client(Subscription subscription) {
        if (http2Client.isPresent()) {
            logger.info("Using http/2 for {}.", subscription.getQualifiedName());
            return http2Client.get();
        } else {
            logger.info("Using http/1.1 for {}. Http/2 delivery is not enabled on this server.",
                    subscription.getQualifiedName());
            return httpClient;
        }
    }

    @Override
    public void start() throws Exception {
        startClient(httpClient);
        http2Client.ifPresent(this::startClient);
    }

    private void startClient(HttpClient client) {
        if (client.isStopped()) {
            try {
                client.start();
            } catch (Exception ex) {
                logger.error("Could not start http client.", ex);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        stopClient(httpClient);
        http2Client.ifPresent(this::stopClient);
    }

    private void stopClient(HttpClient client) {
        if (client.isRunning()) {
            try {
                client.stop();
            } catch (Exception ex) {
                logger.error("Could not stop http client", ex);
            }
        }
    }
}
