package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.net.URI;

public class ResolvableEndpointAddress {

    private final EndpointAddress address;

    private final EndpointAddressResolver resolver;

    public ResolvableEndpointAddress(EndpointAddress address, EndpointAddressResolver resolver) {
        this.address = address;
        this.resolver = resolver;
    }

    public URI resolveFor(Message message) throws EndpointAddressResolutionException {
        return resolver.resolve(address, message);
    }

    public String toString() {
        return address.toString();
    }

}
