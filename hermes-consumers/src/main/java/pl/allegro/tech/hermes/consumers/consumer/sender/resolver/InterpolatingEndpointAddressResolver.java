package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import java.net.URI;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.InterpolationException;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;

public class InterpolatingEndpointAddressResolver implements EndpointAddressResolver {

  private final UriInterpolator interpolator;

  public InterpolatingEndpointAddressResolver(UriInterpolator interpolator) {
    this.interpolator = interpolator;
  }

  @Override
  public URI resolve(
      EndpointAddress address, Message message, EndpointAddressResolverMetadata metadata)
      throws EndpointAddressResolutionException {
    try {
      return interpolator.interpolate(address, message);
    } catch (InterpolationException ex) {
      throw new EndpointAddressResolutionException(address, ex);
    }
  }
}
