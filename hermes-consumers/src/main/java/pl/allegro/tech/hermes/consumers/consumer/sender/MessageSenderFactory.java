package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

public class MessageSenderFactory {

    private final MessageSenderProviders messageSenderProviders;

    @Inject
    public MessageSenderFactory(
            MessageSenderProviders messageSenderProviders,
            @Named("defaultHttpMessageSenderProvider") ProtocolMessageSenderProvider defaultHttpMessageSenderProvider,
            @Named("defaultJmsMessageSenderProvider") ProtocolMessageSenderProvider defaultJmsMessageSenderProvider) {

        this.messageSenderProviders = messageSenderProviders;
        this.messageSenderProviders.putIfProtocolAbsent("http", defaultHttpMessageSenderProvider);
        this.messageSenderProviders.putIfProtocolAbsent("https", defaultHttpMessageSenderProvider);
        this.messageSenderProviders.putIfProtocolAbsent("jms", defaultJmsMessageSenderProvider);

        this.messageSenderProviders.startAll();
    }

    public MessageSender create(Subscription subscription) {
        EndpointAddress endpoint = subscription.getEndpoint();

        Optional<ProtocolMessageSenderProvider> protocolMessageSenderProvider = messageSenderProviders.get(endpoint.getProtocol());
        if (!protocolMessageSenderProvider.isPresent()) {
            throw new EndpointProtocolNotSupportedException(endpoint);
        }

        return protocolMessageSenderProvider.get().create(endpoint.getEndpoint());
    }
}
