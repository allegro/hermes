package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.ApacheHttpClientMessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.DefaultBatchHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;

import java.time.Duration;

import static com.google.common.base.Preconditions.checkState;

public class HttpMessageBatchSenderFactory implements MessageBatchSenderFactory {

    private final SendingResultHandlers resultHandlers;
    private final Duration connectionTimeout;
    private final Duration connectionRequestTimeout;

    public HttpMessageBatchSenderFactory(SendingResultHandlers resultHandlers,
                                         Duration connectionTimeout,
                                         Duration connectionRequestTimeout) {
        this.resultHandlers = resultHandlers;
        this.connectionTimeout = connectionTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    @Override
    public MessageBatchSender create(Subscription subscription) {
        checkState(subscription.getEndpoint().getProtocol().contains("http"), "Batching is only supported for http/s currently.");
        return new ApacheHttpClientMessageBatchSender(
                connectionTimeout,
                connectionRequestTimeout,
                new SimpleEndpointAddressResolver(),
                resultHandlers,
                new DefaultBatchHeadersProvider());
    }
}
