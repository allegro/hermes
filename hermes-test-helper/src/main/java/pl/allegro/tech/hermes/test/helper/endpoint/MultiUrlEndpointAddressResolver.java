package pl.allegro.tech.hermes.test.helper.endpoint;

import com.google.common.base.Throwables;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;

public class MultiUrlEndpointAddressResolver implements EndpointAddressResolver {

  private final EndpointAddressResolver delegate =
      new InterpolatingEndpointAddressResolver(new MessageBodyInterpolator());

  @Override
  public List<URI> resolveAll(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata) {
    return Stream.of(address.getEndpoint().split(";"))
        .map(url -> safeResolve(message, url, metadata))
        .collect(Collectors.toList());
  }

  @Override
  public URI resolve(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    return delegate.resolve(address, message, metadata);
  }

  @Override
  public URI resolve(
      EndpointAddress address, MessageBatch batch, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    return delegate.resolve(address, batch, metadata);
  }

  private URI safeResolve(Message message, String url, EndpointAddressResolverMetadata metadata) {
    try {
      return delegate.resolve(EndpointAddress.of(url), message, metadata);
    } catch (EndpointAddressResolutionException e) {
      throw Throwables.propagate(e);
    }
  }
}
