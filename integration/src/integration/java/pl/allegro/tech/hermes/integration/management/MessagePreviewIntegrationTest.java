package pl.allegro.tech.hermes.integration.management;

import com.jayway.awaitility.Duration;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.util.List;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.*;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class MessagePreviewIntegrationTest extends IntegrationTest {

    public static final String ALL = "{\"query\": {}}";

    @BeforeMethod
    public void clearTopics() {
        management.query().queryTopics(ALL).forEach(topic ->
                management.topic().remove(topic.getQualifiedName()));
        wait.until(() -> management.query().queryTopics(ALL).isEmpty());
    }

    @Test
    public void shouldReturnAvroMessageWithSchemaAwareSerialization() {
        // given
        AvroUser avroUser = new AvroUser("Bob", 50, "blue");
        Topic topic = randomTopic("avro", "returnSchemaAwareSerialization")
                .withSchemaIdAwareSerialization()
                .withContentType(AVRO)
                .build();
        TopicWithSchema topicWithSchema = topicWithSchema(topic, avroUser.getSchemaAsString());
        operations.buildTopicWithSchema(topicWithSchema);
        publisher.publish(topic.getQualifiedName(), avroUser.asBytes());
        wait.awaitAtMost(Duration.TEN_SECONDS).until(() -> management.topic().getPreview(topic.getQualifiedName()).size() == 1);

        // when
        List<MessageTextPreview> previews = management.topic().getPreview(topic.getQualifiedName());

        // then
        assertThat(previews).hasSize(1);
        assertThatJson(previews.get(0).getContent())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(avroUser.asJson());
    }
}
