package pl.allegro.tech.hermes.test.helper.endpoint;

import com.google.common.base.Throwables;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.MessageBodyInterpolator;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.InterpolatingEndpointAddressResolver;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiUrlEndpointAddressResolver implements EndpointAddressResolver {

    private final EndpointAddressResolver delegate = new InterpolatingEndpointAddressResolver(new MessageBodyInterpolator());

    @Override
    public List<URI> resolveAll(EndpointAddress address, Message message) throws EndpointAddressResolutionException {
        return Stream.of(address.getEndpoint().split(";"))
                .map(url -> safeResolve(message, url))
                .collect(Collectors.toList());
    }

    @Override
    public URI resolve(EndpointAddress address, Message message) throws EndpointAddressResolutionException {
        return delegate.resolve(address, message);
    }

    @Override
    public URI resolve(EndpointAddress address, MessageBatch batch) throws EndpointAddressResolutionException {
        return delegate.resolve(address, batch);
    }

    private URI safeResolve(Message message, String url) {
        try {
            return delegate.resolve(EndpointAddress.of(url), message);
        } catch (EndpointAddressResolutionException e) {
            throw Throwables.propagate(e);
        }
    }

}
