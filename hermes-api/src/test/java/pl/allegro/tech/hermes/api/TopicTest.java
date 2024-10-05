package pl.allegro.tech.hermes.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class TopicTest {

  private final ObjectMapper objectMapper = createObjectMapper(false);

  @Test
  public void shouldDeserializeTopicWithDefaults() throws Exception {
    // given
    String json = "{\"name\":\"foo.bar\", \"description\": \"description\"}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.getName().getName()).isEqualTo("bar");
    assertThat(topic.getName().getGroupName()).isEqualTo("foo");
    assertThat(topic.getDescription()).isEqualTo("description");
    assertThat(topic.isSchemaIdAwareSerializationEnabled()).isEqualTo(true);
  }

  @Test
  public void shouldDeserializeTopic() throws Exception {
    // given
    String json =
        "{\"name\":\"foo.bar\", \"description\": \"description\", \"schemaIdAwareSerializationEnabled\": \"false\"}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.getName().getName()).isEqualTo("bar");
    assertThat(topic.getName().getGroupName()).isEqualTo("foo");
    assertThat(topic.getDescription()).isEqualTo("description");
    assertThat(topic.isSchemaIdAwareSerializationEnabled()).isEqualTo(false);
  }

  @Test
  public void shouldSetDefaultAckIfNotPresentInJson() throws Exception {
    // given
    String json = "{\"name\":\"foo.bar\", \"description\": \"description\"}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.isReplicationConfirmRequired()).isEqualTo(false);
  }

  @Test
  public void shouldSkippedDeserializedOldSchemaVersionId() throws Exception {
    // given
    String json =
        "{\"name\":\"foo.bar\", \"description\": \"description\", \"schemaVersionAwareSerializationEnabled\": false}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.getName().getName()).isEqualTo("bar");
  }

  @Test
  public void shouldDeserializeFallbackToRemoteDatacenterWithDefaults() throws Exception {
    // given
    String json = "{\"name\":\"foo.bar\", \"description\": \"description\"}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.isFallbackToRemoteDatacenterEnabled()).isEqualTo(false);

    // and when
    Topic topic2 = createObjectMapper(true).readValue(json, Topic.class);

    // then
    assertThat(topic2.isFallbackToRemoteDatacenterEnabled()).isEqualTo(true);
  }

  @Test
  public void shouldDeserializeFallbackToRemoteDatacenter() throws Exception {
    // given
    String json =
        "{\"name\":\"foo.bar\", \"description\": \"description\", \"fallbackToRemoteDatacenterEnabled\": true}";

    // when
    Topic topic = objectMapper.readValue(json, Topic.class);

    // then
    assertThat(topic.isFallbackToRemoteDatacenterEnabled()).isEqualTo(true);
  }

  private ObjectMapper createObjectMapper(boolean fallbackToRemoteDatacenterEnabled) {
    ObjectMapper mapper = new ObjectMapper();

    final InjectableValues defaultSchemaIdAwareSerializationEnabled =
        new InjectableValues.Std()
            .addValue(Topic.DEFAULT_SCHEMA_ID_SERIALIZATION_ENABLED_KEY, true)
            .addValue(
                Topic.DEFAULT_FALLBACK_TO_REMOTE_DATACENTER_KEY, fallbackToRemoteDatacenterEnabled);

    mapper.setInjectableValues(defaultSchemaIdAwareSerializationEnabled);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
}
