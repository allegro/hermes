package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion.valueOf;

public class MessageContentWrapperTest {

    private final MessageContentWrapper messageContentWrapper = new MessageContentWrapper(
            new JsonMessageContentWrapper("message", "metadata", new ObjectMapper()),
            new AvroMessageContentWrapper(Clock.systemDefaultZone())
    );

    private final Function<Topic, List<SchemaVersion>> schemaVersionsProvider = topic -> asList(
            valueOf(2),
            valueOf(1)
    );

    @Test
    public void shouldUseOlderSchemaToUnwrapMessage() throws IOException {
        // given
        AvroUser avroUser = new AvroUser("Bob", 15, "blue");
        Topic topic = TopicBuilder.topic("group", "topic").build();

        Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider = schemaVersion -> {
            if (schemaVersion.get().equals(valueOf(1))) {
                return new CompiledSchema<>(avroUser.getSchema(), schemaVersion.get());
            } else if (schemaVersion.get().equals(valueOf(2))) {
                return new CompiledSchema<>(AvroUserSchemaLoader.load("/schema/user_v2.avsc"), valueOf(2));
            }
            throw new RuntimeException("Unknown version");
        };

        byte [] wrapped = messageContentWrapper.wrapAvro(
                avroUser.asBytes(), "uniqueId", 1234, topic, schemaProvider.apply(of(valueOf(1))), new HashedMap<>());

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(
                wrapped, topic, schemaProvider, schemaVersionsProvider);

        // then
        assertThat(unwrappedMessageContent.getContent()).contains(avroUser.asBytes());
        assertThat(unwrappedMessageContent.getSchema().get().getVersion().value()).isEqualTo(1);
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo("uniqueId");
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(1234);
    }

    @Test
    public void shouldUseLatestSchemaToUnwrapMessage() throws IOException {
        // given
        AvroUser avroUser = new AvroUser("Bob", 15, "blue");
        Topic topic = TopicBuilder.topic("group", "topic").build();

        Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider = schemaVersion -> {
            if (schemaVersion.get().equals(valueOf(2))) {
                return new CompiledSchema<>(avroUser.getSchema(), schemaVersion.get());
            } else if (schemaVersion.get().equals(valueOf(1))) {
                return new CompiledSchema<>(AvroUserSchemaLoader.load("/schema/user_v2.avsc"), valueOf(2));
            }
            throw new RuntimeException("Unknown version");
        };

        byte [] wrapped = messageContentWrapper.wrapAvro(
                avroUser.asBytes(), "uniqueId", 1234, topic, schemaProvider.apply(of(valueOf(2))), new HashedMap<>());

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(
                wrapped, topic, schemaProvider, schemaVersionsProvider);

        // then
        assertThat(unwrappedMessageContent.getContent()).contains(avroUser.asBytes());
        assertThat(unwrappedMessageContent.getSchema().get().getVersion().value()).isEqualTo(2);
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo("uniqueId");
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(1234);
    }

    @Test
    public void shouldThrowExceptionWhenMessageCouldNotBeUnwrappedByAnySchema() throws IOException {
        // given
        AvroUser avroUser = new AvroUser("Bob", 15, "blue");
        Topic topic = TopicBuilder.topic("group", "topic").build();

        Function<Optional<SchemaVersion>, CompiledSchema<Schema>> schemaProvider = schemaVersion ->
                new CompiledSchema<>(AvroUserSchemaLoader.load("/schema/user_v2.avsc"), valueOf(2));

        Function<Topic, List<SchemaVersion>> schemaVersionsProvider = t -> asList(valueOf(2));

        byte [] wrapped = messageContentWrapper.wrapAvro(
                avroUser.asBytes(), "uniqueId", 1234, topic, avroUser.getCompiledSchema(), new HashedMap<>());

        // when
        catchException(messageContentWrapper).unwrapAvro(wrapped, topic, schemaProvider, schemaVersionsProvider);

        // then
        assertThat(caughtException().getMessage()).isEqualTo("Schema for topic " + topic.getQualifiedName() + " was not available");
    }

}