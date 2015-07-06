package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.net.URI;

public interface EndpointAddressResolver {

    URI resolve(EndpointAddress address, Message message) throws EndpointAddressResolutionException;

}
