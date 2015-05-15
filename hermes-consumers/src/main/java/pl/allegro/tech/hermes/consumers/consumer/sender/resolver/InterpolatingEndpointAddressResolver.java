package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.InterpolationException;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import javax.inject.Inject;
import java.net.URI;

public class InterpolatingEndpointAddressResolver implements EndpointAddressResolver {

    private final UriInterpolator interpolator;

    @Inject
    public InterpolatingEndpointAddressResolver(UriInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public URI resolve(EndpointAddress address, Message message) throws EndpointAddressResolutionException {
        try {
            return interpolator.interpolate(address, message);
        } catch (InterpolationException ex) {
            throw new EndpointAddressResolutionException(address, ex);
        }
    }

}
