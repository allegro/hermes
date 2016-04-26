package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMode;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import javax.inject.Inject;

public class JettyHttpMessageSenderProvider implements ProtocolMessageSenderProvider {

    private final HttpClient httpClient;
    private final EndpointAddressResolver endpointAddressResolver;
    private final MetadataAppender<Request> metadataAppender;
    private final HttpAuthorizationProviderFactory authorizationProviderFactory;

    @Inject
    public JettyHttpMessageSenderProvider(
            HttpClient httpClient,
            EndpointAddressResolver endpointAddressResolver,
            MetadataAppender<Request> metadataAppender,
            HttpAuthorizationProviderFactory authorizationProviderFactory) {
        this.httpClient = httpClient;
        this.endpointAddressResolver = endpointAddressResolver;
        this.metadataAppender = metadataAppender;
        this.authorizationProviderFactory = authorizationProviderFactory;
    }

    @Override
    public MessageSender create(Subscription subscription) {
        EndpointAddress endpoint = subscription.getEndpoint();
        ResolvableEndpointAddress resolvableEndpoint = new ResolvableEndpointAddress(endpoint, endpointAddressResolver);
        HttpRequestFactory requestFactory = httpRequestFactory(subscription);

        if (subscription.getMode() == SubscriptionMode.BROADCAST) {
            return new JettyBroadCastMessageSender(requestFactory, resolvableEndpoint);
        } else {
            return new JettyMessageSender(requestFactory, resolvableEndpoint);
        }
    }

    private HttpRequestFactory httpRequestFactory(Subscription subscription) {
        EndpointAddress endpoint = subscription.getEndpoint();
        int requestTimeout = subscription.getSerialSubscriptionPolicy().getRequestTimeout();
        return new HttpRequestFactory(httpClient, requestTimeout, metadataAppender, authorizationProviderFactory.create(endpoint));
    }


    @Override
    public void start() throws Exception {
        if (httpClient.isStopped()) {
            httpClient.start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (httpClient.isRunning()) {
            httpClient.stop();
        }
    }
}
