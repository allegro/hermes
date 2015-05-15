package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;

@SuppressWarnings("serial")
public class EndpointAddressResolutionException extends Exception {

    public EndpointAddressResolutionException(EndpointAddress endpointAddress, Throwable cause) {
        super("Failed to resolve " + endpointAddress, cause);
    }

    public EndpointAddressResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
