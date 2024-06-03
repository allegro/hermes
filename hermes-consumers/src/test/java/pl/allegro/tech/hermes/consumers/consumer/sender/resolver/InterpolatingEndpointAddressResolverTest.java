package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.InterpolationException;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

public class InterpolatingEndpointAddressResolverTest {

    private final UriInterpolator interpolator = mock(UriInterpolator.class);

    private final InterpolatingEndpointAddressResolver resolver = new InterpolatingEndpointAddressResolver(interpolator);

    private final EndpointAddressResolverMetadata metadata = EndpointAddressResolverMetadata.empty();

    @Test
    public void shouldUseInterpolatorToInterpolateURI() throws InterpolationException, EndpointAddressResolutionException {
        // given
        EndpointAddress address = EndpointAddress.of("http://localhost/{a}");
        Message message = withTestMessage().withContent("content", StandardCharsets.UTF_8).build();
        when(interpolator.interpolate(address, message)).thenReturn(URI.create("http://localhost/hello"));

        // when
        URI uri = resolver.resolve(EndpointAddress.of("http://localhost/{a}"),
                withTestMessage().withContent("content", StandardCharsets.UTF_8).build(), metadata);

        // then
        assertThat(uri).isEqualTo(URI.create("http://localhost/hello"));
    }

    @Test
    public void shouldThrowResolvingExceptionWhenInterpolationFails() throws InterpolationException {
        // given
        EndpointAddress address = EndpointAddress.of("http://localhost/{a}");
        Message message = withTestMessage().withContent("content", StandardCharsets.UTF_8).build();
        when(interpolator.interpolate(address, message)).thenThrow(InterpolationException.class);

        // then
        assertThrows(EndpointAddressResolutionException.class,
                () -> resolver.resolve(
                        EndpointAddress.of("http://localhost/{a}"),
                        withTestMessage().withContent("content", StandardCharsets.UTF_8).build(),
                        metadata)
        );
    }
}
