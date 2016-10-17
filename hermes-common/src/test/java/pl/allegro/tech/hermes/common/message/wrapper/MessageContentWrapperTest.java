package pl.allegro.tech.hermes.common.message.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;

public class MessageContentWrapperTest {

    private final MessageContentWrapper messageContentWrapper = new MessageContentWrapper(
            new JsonMessageContentWrapper("message", "metadata", new ObjectMapper()),
            new AvroMessageContentWrapper(Clock.systemDefaultZone()), schemaRepository);

    static Topic topic = TopicBuilder.topic("group", "topic").build();

    static CompiledSchema<Schema> schema1 = new CompiledSchema<>(load("/schema/user.avsc"), SchemaVersion.valueOf(1));
    static CompiledSchema<Schema> schema2 = new CompiledSchema<>(load("/schema/user_v2.avsc"), SchemaVersion.valueOf(2));
    static CompiledSchema<Schema> schema3 = new CompiledSchema<>(load("/schema/user_v3.avsc"), SchemaVersion.valueOf(3));

    static SchemaVersionsRepository schemaVersionsRepository = new SchemaVersionsRepository() {
        @Override
        public List<SchemaVersion> versions(Topic topic, boolean online) {
            return online? asList(schema3.getVersion(), schema2.getVersion(), schema1.getVersion())
                            : asList(schema2.getVersion(), schema1.getVersion());
        }

        @Override
        public void close() {
            // nothing to close
        }
    };

    static CompiledSchemaRepository<Schema> compiledSchemaRepository = (topic, version) -> {
        switch (version.value()) {
            case 1: return schema1;
            case 2: return schema2;
            case 3: return schema3;
            default: throw new RuntimeException("sry");}
    };
    static SchemaRepository schemaRepository = new SchemaRepository(schemaVersionsRepository, compiledSchemaRepository);

    @Test
    public void shouldUnwrapMessageUsingEverySchemaAvailable() throws IOException {
        // forcing offline latest
        shouldUnwrapMessageWrappedWithSchemaAtVersion(SchemaVersion.valueOf(2));

        // forcing fallback
        shouldUnwrapMessageWrappedWithSchemaAtVersion(SchemaVersion.valueOf(1));

        // forcing online check
        shouldUnwrapMessageWrappedWithSchemaAtVersion(SchemaVersion.valueOf(3));
    }

    public void shouldUnwrapMessageWrappedWithSchemaAtVersion(SchemaVersion version) throws IOException {
        // given
        CompiledSchema<Schema> schema = schemaRepository.getKnownAvroSchemaVersion(topic, version);
        AvroUser user = new AvroUser(schema, "Bob", 15, "blue");
        byte[] wrapped = messageContentWrapper.wrapAvro(user.asBytes(), "uniqueId", 1234, topic, schema, new HashedMap<>());

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic);

        // then
        assertThat(unwrappedMessageContent.getContent()).contains(user.asBytes());
        assertThat(unwrappedMessageContent.getSchema().get().getVersion().value()).isEqualTo(version.value());
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo("uniqueId");
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(1234);
    }

    @Test(expected = SchemaMissingException.class)
    public void shouldThrowExceptionWhenMessageCouldNotBeUnwrappedByAnySchema() throws IOException {
        // given
        byte[] doesNotMatchAnySchema = "{}".getBytes();

        // when
        messageContentWrapper.unwrapAvro(doesNotMatchAnySchema, topic);
    }
}