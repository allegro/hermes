package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;

public interface EndpointAddressResolver {

  default URI resolve(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    return resolve(address);
  }

  default URI resolve(
      EndpointAddress address, MessageBatch batch, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    return resolve(address);
  }

  static URI resolve(EndpointAddress address) throws EndpointAddressResolutionException {
    try {
      return address.getUri();
    } catch (Exception ex) {
      throw new EndpointAddressResolutionException(address, ex);
    }
  }

  default List<URI> resolveAll(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    return Collections.singletonList(resolve(address, message, metadata));
  }
}
