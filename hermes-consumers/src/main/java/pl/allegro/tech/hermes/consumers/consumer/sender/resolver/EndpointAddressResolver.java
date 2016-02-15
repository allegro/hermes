package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;

import java.net.URI;

public interface EndpointAddressResolver {

    default URI resolve(EndpointAddress address, Message message) throws EndpointAddressResolutionException {
        return resolve(address);
    }

    default URI resolve(EndpointAddress address, MessageBatch batch) throws EndpointAddressResolutionException {
        return resolve(address);
    }

    static URI resolve(EndpointAddress address) throws EndpointAddressResolutionException {
        try {
            return URI.create(address.getEndpoint());
        } catch (Exception ex) {
            throw new EndpointAddressResolutionException(address, ex);
        }
    }
}