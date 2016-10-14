package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TopicTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldDeserializeTopic() throws Exception {
        // given
        String json = "{\"name\":\"foo.bar\", \"description\": \"description\"}";

        // when
        Topic topic = objectMapper.readValue(json, Topic.class);

        // then
        assertThat(topic.getName().getName()).isEqualTo("bar");
        assertThat(topic.getName().getGroupName()).isEqualTo("foo");
        assertThat(topic.getDescription()).isEqualTo("description");
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
}
