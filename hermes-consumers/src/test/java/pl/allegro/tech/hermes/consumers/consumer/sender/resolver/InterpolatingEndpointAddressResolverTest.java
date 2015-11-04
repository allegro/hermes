package pl.allegro.tech.hermes.consumers.consumer.sender.resolver;

import com.googlecode.catchexception.CatchException;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.InterpolationException;
import pl.allegro.tech.hermes.consumers.consumer.interpolation.UriInterpolator;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

public class InterpolatingEndpointAddressResolverTest {

    private final UriInterpolator interpolator = mock(UriInterpolator.class);

    private final InterpolatingEndpointAddressResolver resolver = new InterpolatingEndpointAddressResolver(interpolator);

    @Test
    public void shouldUseInterpolatorToInterpolateURI() throws InterpolationException, EndpointAddressResolutionException {
        // given
        EndpointAddress address = EndpointAddress.of("http://localhost/{a}");
        Message message = withTestMessage().withContent("content", StandardCharsets.UTF_8).build();
        when(interpolator.interpolate(address, message)).thenReturn(URI.create("http://localhost/hello"));

        // when
        URI uri = resolver.resolve(EndpointAddress.of("http://localhost/{a}"),
                withTestMessage().withContent("content", StandardCharsets.UTF_8).build());

        // then
        assertThat(uri).isEqualTo(URI.create("http://localhost/hello"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowResolvingExceptionWhenInterpolationFails() throws EndpointAddressResolutionException, InterpolationException {
        // given
        EndpointAddress address = EndpointAddress.of("http://localhost/{a}");
        Message message = withTestMessage().withContent("content", StandardCharsets.UTF_8).build();
        when(interpolator.interpolate(address, message)).thenThrow(InterpolationException.class);

        // when
        catchException(resolver).resolve(EndpointAddress.of("http://localhost/{a}"),
                withTestMessage().withContent("content", StandardCharsets.UTF_8).build());

        // then
        assertThat(CatchException.<EndpointAddressResolutionException>caughtException()).isInstanceOf(EndpointAddressResolutionException.class);
    }

}
