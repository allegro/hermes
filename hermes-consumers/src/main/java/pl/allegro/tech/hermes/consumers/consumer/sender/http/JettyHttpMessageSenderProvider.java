package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.api.EndpointAddress.of;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_REQUEST_TIMEOUT;

public class JettyHttpMessageSenderProvider implements ProtocolMessageSenderProvider {

    private final HttpClient httpClient;
    private final EndpointAddressResolver endpointAddressResolver;
    private final int requestTimeout;
    private final DefaultHttpTraceIdAppender traceIdAppender;

    @Inject
    public JettyHttpMessageSenderProvider(
            HttpClient httpClient,
            ConfigFactory configFactory,
            EndpointAddressResolver endpointAddressResolver,
            DefaultHttpTraceIdAppender traceIdAppender) {

        this.httpClient = httpClient;
        this.endpointAddressResolver = endpointAddressResolver;
        this.requestTimeout = configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_REQUEST_TIMEOUT);
        this.traceIdAppender = traceIdAppender;
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

    @Override
    public MessageSender create(String endpoint) {
        ResolvableEndpointAddress resolvableEndpoint = new ResolvableEndpointAddress(of(endpoint), endpointAddressResolver);
        return new JettyMessageSender(httpClient, resolvableEndpoint, requestTimeout, traceIdAppender);
    }
}
