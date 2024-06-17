package pl.allegro.tech.hermes.integrationtests.management;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.time.Duration;
import java.util.List;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class MessagePreviewIntegrationTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldReturnAvroMessageWithSchemaAwareSerialization() {
        // given
        AvroUser avroUser = new AvroUser("Bob", 50, "blue");
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .withSchemaIdAwareSerialization()
                .build(), avroUser.getSchemaAsString());

        Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

        hermes.api().publishAvroUntilSuccess(topic.getQualifiedName(), avroUser.asBytes());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                    // when
                    List<MessageTextPreview> previews = hermes.api().getPreview(topic.getQualifiedName())
                            .expectStatus().isOk()
                            .expectBodyList(MessageTextPreview.class).returnResult().getResponseBody();

                    // then
                    assertThat(previews).hasSize(1);
                    assertThatJson(previews.get(0).getContent())
                            .when(Option.IGNORING_EXTRA_FIELDS)
                            .isEqualTo(avroUser.asJson());
                }
        );
    }
}
