package pl.allegro.tech.hermes.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

public class TopicWithSchemaTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void shouldSerializeSchemaVersionMetadataWhenPresent() throws Exception {
    Topic topic = TopicBuilder.topic("group.topic").build();
    TopicWithSchema topicWithSchema =
        TopicWithSchema.topicWithSchema(topic, "schema", 3, List.of(1, 2, 3), "namespace_group.topic-value");

    String serialized = objectMapper.writeValueAsString(topicWithSchema);

    assertThat(serialized)
        .contains(
            "\"schemaVersion\":3",
            "\"availableSchemaVersions\":[1,2,3]",
            "\"schemaSubject\":\"namespace_group.topic-value\"");
  }

  @Test
  public void shouldOmitAbsentOrEmptySchemaVersionMetadata() throws Exception {
    Topic topic = TopicBuilder.topic("group.topic").build();

    String empty =
        objectMapper.writeValueAsString(
            TopicWithSchema.topicWithSchema(
                topic, "schema", null, List.of(), "namespace_group.topic-value"));

    assertThat(empty)
        .doesNotContain("schemaVersion", "availableSchemaVersions")
        .contains("\"schemaSubject\":\"namespace_group.topic-value\"");
  }
}
