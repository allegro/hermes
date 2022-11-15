package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.DefaultBatchHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;

import static com.google.common.base.Preconditions.checkState;

public class HttpMessageBatchSenderFactory implements MessageBatchSenderFactory {

    private final SendingResultHandlers resultHandlers;
    private final HttpClient httpClient;

    public HttpMessageBatchSenderFactory(SendingResultHandlers resultHandlers,
                                         HttpClient httpClient
    ) {
        this.resultHandlers = resultHandlers;
        this.httpClient = started(httpClient);
    }


    @Override
    public MessageBatchSender create(Subscription subscription) {
        checkState(subscription.getEndpoint().getProtocol().contains("http"), "Batching is only supported for http/s currently.");


        return new JettyMessageBatchSender(
                new DefaultBatchHttpRequestFactory(httpClient),
                new SimpleEndpointAddressResolver(),
                resultHandlers,
                new DefaultBatchHeadersProvider());
    }

    private static HttpClient started(HttpClient httpClient) {
        try {
            httpClient.start();
            return httpClient;
        } catch (Exception e) {
            throw new InternalProcessingException("Failed to start http batch sender", e);
        }
    }


    @Override
    public void close() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            throw new InternalProcessingException("Failed to stop http batch sender", e);
        }
    }
}
