package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.ApacheHttpClientMessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.SendingResultHandlers;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.DefaultBatchHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;

import static com.google.common.base.Preconditions.checkState;

public class HttpMessageBatchSenderFactory implements MessageBatchSenderFactory {
    private final ConfigFactory configFactory;
    private final SendingResultHandlers resultHandlers;

    public HttpMessageBatchSenderFactory(ConfigFactory configFactory, SendingResultHandlers resultHandlers) {
        this.configFactory = configFactory;
        this.resultHandlers = resultHandlers;
    }

    @Override
    public MessageBatchSender create(Subscription subscription) {
        checkState(subscription.getEndpoint().getProtocol().contains("http"), "Batching is only supported for http/s currently.");
        return new ApacheHttpClientMessageBatchSender(
                configFactory.getIntProperty(Configs.CONSUMER_BATCH_CONNECTION_TIMEOUT),
                configFactory.getIntProperty(Configs.CONSUMER_BATCH_CONNECTION_REQUEST_TIMEOUT),
                new SimpleEndpointAddressResolver(),
                resultHandlers,
                new DefaultBatchHeadersProvider());
    }
}
