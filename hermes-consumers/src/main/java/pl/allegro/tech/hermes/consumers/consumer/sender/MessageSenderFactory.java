package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.HashMap;
import java.util.Map;

public class MessageSenderFactory {

    private final Map<String, ProtocolMessageSenderProvider> protocolProviders = new HashMap<>();

    public MessageSender create(Subscription subscription) {
        EndpointAddress endpoint = subscription.getEndpoint();

        ProtocolMessageSenderProvider provider = protocolProviders.get(endpoint.getProtocol());
        if (provider == null) {
            throw new EndpointProtocolNotSupportedException(endpoint);
        }
        return provider.create(subscription);
    }

    public void addSupportedProtocol(String protocol, ProtocolMessageSenderProvider provider) {
        if (!protocolProviders.containsKey(protocol)) {
            overrideProtocol(protocol, provider);
        }
    }

    public void overrideProtocol(String protocol, ProtocolMessageSenderProvider provider) {
        startProvider(provider);
        protocolProviders.put(protocol, provider);
    }

    public void startProvider(ProtocolMessageSenderProvider provider) {
        try {
            provider.start();
        } catch (Exception e) {
            throw new InternalProcessingException("Something went wrong while starting message sender provider", e);
        }
    }

    public void closeProviders() {
        protocolProviders.values().forEach(provider -> {
            try {
                provider.stop();
            } catch (Exception e) {
                throw new InternalProcessingException("Something went wrong while stopping message sender provider", e);
            }
        });
    }
}
