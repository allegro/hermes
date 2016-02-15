package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.ApacheHttpClientMessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;

public class HttpMessageBatchSenderFactory implements MessageBatchSenderFactory {
    private ConfigFactory configFactory;

    @Inject
    public HttpMessageBatchSenderFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public MessageBatchSender create(Subscription subscription) {
        checkState(subscription.getEndpoint().getProtocol().contains("http"), "Batching is only supported for http/s currently.");
        return new ApacheHttpClientMessageBatchSender(
                configFactory.getIntProperty(Configs.CONSUMER_BATCH_CONNECTION_TIMEOUT),
                configFactory.getIntProperty(Configs.CONSUMER_BATCH_SOCKET_TIMEOUT),
                new SimpleEndpointAddressResolver());
    }
}
