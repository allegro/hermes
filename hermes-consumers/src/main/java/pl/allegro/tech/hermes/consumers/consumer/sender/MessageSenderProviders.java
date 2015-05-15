package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageSenderProviders {

    private final Map<String, ProtocolMessageSenderProvider> messageSendersProviders = new HashMap<>();

    public void put(String protocol, ProtocolMessageSenderProvider provider) {
        messageSendersProviders.put(protocol, provider);
    }

    public void putIfProtocolAbsent(String protocol, ProtocolMessageSenderProvider provider) {
        messageSendersProviders.putIfAbsent(protocol, provider);
    }

    public Optional<ProtocolMessageSenderProvider> get(String protocol) {
        return Optional.ofNullable(messageSendersProviders.get(protocol));
    }

    public void startAll() {
        messageSendersProviders.values().forEach(provider -> {
            try {
                provider.start();
            } catch (Exception e) {
                throw new InternalProcessingException("Something went wrong while starting message sender provider", e);
            }
        });
    }

    public void stopAll() {
        messageSendersProviders.values().forEach(provider -> {
            try {
                provider.stop();
            } catch (Exception e) {
                throw new InternalProcessingException("Something went wrong while stopping message sender provider", e);
            }
        });
    }
}
