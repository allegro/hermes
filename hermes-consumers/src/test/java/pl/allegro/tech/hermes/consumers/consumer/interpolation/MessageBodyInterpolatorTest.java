package pl.allegro.tech.hermes.consumers.consumer.interpolation;

import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageBodyInterpolatorTest {

    private static final Message SAMPLE_MSG = new Message(
            "id", 0, 0, "some.topic", "{\"a\": \"b\"}".getBytes(), 214312123L, 2143121233L
    );

    @Test
    public void willReturnURIAsIsIfNoTemplate() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/");

        // when
        URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);

        // then
        assertThat(interpolated).isEqualTo(URI.create(endpoint.getEndpoint()));
    }

    @Test
    public void willInterpolateJsonPathFromTemplate() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}");
        URI expectedEndpoint = URI.create("http://some.endpoint.com/100");
        String jsonMessage = "{\"some\": {\"object\": 100}}";
        Message msg = new Message("id", 0, 0, "some.topic", jsonMessage.getBytes(), 121422L, 121423L);

        // when
        URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, msg);

        // then
        assertThat(interpolated).isEqualTo(expectedEndpoint);
    }

    @Test
    public void willReturnURIOnEmptyEndpoint() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("");
        URI expectedEndpoint = URI.create("");

        // when
        URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);

        // then
        assertThat(interpolated).isEqualTo(expectedEndpoint);
    }

    @Test
    public void willInterpolateMultipleJsonPathsFromTemplate() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");
        URI expectedEndpoint = URI.create("http://some.endpoint.com/100?test=hello");
        String jsonMessage = "{\"some\": {\"object\": 100, \"test\": \"hello\"}}";
        Message msg = new Message("id", 0, 0, "some.topic", jsonMessage.getBytes(), 12323L, 123234L);


        // when
        URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, msg);

        // then
        assertThat(interpolated).isEqualTo(expectedEndpoint);
    }

    @Test(expected = InterpolationException.class)
    public void willThrowExceptionOnInvalidPayload() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}");

        // when
        new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);
    }

    @Test(expected = InterpolationException.class)
    public void willThrowExceptionOnInterpolationException() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");

        // when
        new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);
    }

    @Test
    public void willInterpolateMultipleJsonPathsFromTemplateInReverseOrder() throws InterpolationException {
        // given
        EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");
        URI expectedEndpoint = URI.create("http://some.endpoint.com/100?test=hello");
        String jsonMessage = "{\"some\": {\"test\": \"hello\", \"object\": 100}}";
        Message msg = new Message("id", 0, 0, "some.topic", jsonMessage.getBytes(), 1232443L, 12324434L);

        // when
        URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, msg);

        // then
        assertThat(interpolated).isEqualTo(expectedEndpoint);
    }
}