package pl.allegro.tech.hermes.consumers.consumer.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class MessageBodyInterpolatorTest {

  private static final Message SAMPLE_MSG =
      withTestMessage()
          .withTopic("some.topic")
          .withContent("{\"a\": \"b\"}", StandardCharsets.UTF_8)
          .withPublishingTimestamp(214312123L)
          .withReadingTimestamp(2143121233L)
          .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 0, 0))
          .build();

  private static final KafkaTopicName KAFKA_TOPIC = KafkaTopicName.valueOf("kafka_topic");

  @Test
  public void willReturnURIAsIsIfNoTemplate() throws InterpolationException {
    // given
    EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/");

    // when
    URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);

    // then
    assertThat(interpolated).isEqualTo(endpoint.getUri());
  }

  @Test
  public void willInterpolateJsonPathFromTemplate() throws InterpolationException {
    // given
    EndpointAddress endpoint = EndpointAddress.of("http://some.endpoint.com/{some.object}");
    URI expectedEndpoint = URI.create("http://some.endpoint.com/100");
    String jsonMessage = "{\"some\": {\"object\": 100}}";
    Message msg =
        withTestMessage()
            .withTopic("some.topic")
            .withContent(jsonMessage, StandardCharsets.UTF_8)
            .withPublishingTimestamp(121422L)
            .withReadingTimestamp(121423L)
            .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 0, 0))
            .build();

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
    EndpointAddress endpoint =
        EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");
    URI expectedEndpoint = URI.create("http://some.endpoint.com/100?test=hello");
    String jsonMessage = "{\"some\": {\"object\": 100, \"test\": \"hello\"}}";
    Message msg =
        withTestMessage()
            .withTopic("some.topic")
            .withContent(jsonMessage, StandardCharsets.UTF_8)
            .withPublishingTimestamp(12323L)
            .withReadingTimestamp(123234L)
            .build();

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
    EndpointAddress endpoint =
        EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");

    // when
    new MessageBodyInterpolator().interpolate(endpoint, SAMPLE_MSG);
  }

  @Test
  public void willInterpolateMultipleJsonPathsFromTemplateInReverseOrder()
      throws InterpolationException {
    // given
    EndpointAddress endpoint =
        EndpointAddress.of("http://some.endpoint.com/{some.object}?test={some.test}");
    URI expectedEndpoint = URI.create("http://some.endpoint.com/100?test=hello");
    String jsonMessage = "{\"some\": {\"test\": \"hello\", \"object\": 100}}";
    Message msg =
        withTestMessage()
            .withTopic("some.topic")
            .withContent(jsonMessage, StandardCharsets.UTF_8)
            .withPublishingTimestamp(1232443L)
            .withReadingTimestamp(12324434L)
            .withPartitionOffset(new PartitionOffset(KAFKA_TOPIC, 0, 0))
            .build();

    // when
    URI interpolated = new MessageBodyInterpolator().interpolate(endpoint, msg);

    // then
    assertThat(interpolated).isEqualTo(expectedEndpoint);
  }
}
