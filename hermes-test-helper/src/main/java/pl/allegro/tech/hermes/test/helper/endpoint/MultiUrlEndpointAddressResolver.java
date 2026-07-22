package pl.allegro.tech.hermes.test.helper.endpoint;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;

public class MultiUrlEndpointAddressResolver implements EndpointAddressResolver {

  @Override
  public List<URI> resolveAll(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata) {
    return Stream.of(address.getEndpoint().split(";"))
        .map(url -> safeResolve(EndpointAddress.of(url)))
        .collect(Collectors.toList());
  }

  private URI safeResolve(EndpointAddress address) {
    try {
      return EndpointAddressResolver.resolve(address);
    } catch (EndpointAddressResolutionException e) {
      throw new RuntimeException(e);
    }
  }
}
