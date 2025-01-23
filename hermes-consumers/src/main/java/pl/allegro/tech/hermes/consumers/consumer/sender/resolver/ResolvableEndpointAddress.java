package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import java.net.URI;
import java.util.List;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class ResolvableEndpointAddress {

  private final EndpointAddress address;

  private final EndpointAddressResolver resolver;

  private final EndpointAddressResolverMetadata metadata;

  public ResolvableEndpointAddress(
      EndpointAddress address,
      EndpointAddressResolver resolver,
      EndpointAddressResolverMetadata metadata) {
    this.address = address;
    this.resolver = resolver;
    this.metadata = metadata;
  }

  public URI resolveFor(Message message) throws EndpointAddressResolutionException {
    return resolver.resolve(address, message, metadata);
  }

  public List<URI> resolveAllFor(Message message) throws EndpointAddressResolutionException {
    return resolver.resolveAll(address, message, metadata);
  }

  public EndpointAddress getRawAddress() {
    return address;
  }

  public String toString() {
    return address.toString();
  }
}
