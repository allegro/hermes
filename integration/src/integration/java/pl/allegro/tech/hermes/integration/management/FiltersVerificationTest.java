package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.ERROR;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.MATCHED;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.NOT_MATCHED;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class FiltersVerificationTest extends IntegrationTest {
    private static final MessageFilterSpecification FILTER_MATCHING_USERS_WITH_NAME_BOB = new MessageFilterSpecification(of(
            "type", "avropath",
            "path", ".name",
            "matcher", "Bob"
    ));

    @Test
    void shouldReturnMatchedWhenGivenMessageMatchesFilter() {
        //given
        Topic topic = createTopicWithAvroUserSchema();
        AvroUser bob = new AvroUser("Bob", 50, "blue");

        //when
        MessageFiltersVerificationResult result = verifyFilters(topic, FILTER_MATCHING_USERS_WITH_NAME_BOB, bob.asJson().getBytes());

        //then
        assertThat(result.getStatus()).isEqualTo(MATCHED);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void shouldReturnNotMatchedWhenGivenMessageDoesNotMatchFilter() {
        //given
        Topic topic = createTopicWithAvroUserSchema();
        AvroUser alice = new AvroUser("Alice", 50, "blue");

        //when
        MessageFiltersVerificationResult result = verifyFilters(topic, FILTER_MATCHING_USERS_WITH_NAME_BOB, alice.asJson().getBytes());

        //then
        assertThat(result.getStatus()).isEqualTo(NOT_MATCHED);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void shouldReturnErrorWhenGivenMessageIsInvalid() {
        //given
        Topic topic = createTopicWithAvroUserSchema();

        //when
        MessageFiltersVerificationResult result = verifyFilters(topic, FILTER_MATCHING_USERS_WITH_NAME_BOB, "xyz".getBytes());

        //then
        assertThat(result.getStatus()).isEqualTo(ERROR);
        assertThat(result.getErrorMessage()).contains("Failed to parse json to map format.");
    }

    private Topic createTopicWithAvroUserSchema() {
        String schema = AvroUserSchemaLoader.load().toString();
        Topic topic = randomTopic("group", "topic")
                .withContentType(AVRO)
                .build();
        operations.buildTopicWithSchema(topicWithSchema(topic, schema));
        return topic;
    }

    private MessageFiltersVerificationResult verifyFilters(Topic topic, MessageFilterSpecification specification, byte[] message) {
        return management.filter().verify(
                topic.getQualifiedName(),
                new MessageFiltersVerificationInput(singletonList(specification), message)
        );
    }
}
