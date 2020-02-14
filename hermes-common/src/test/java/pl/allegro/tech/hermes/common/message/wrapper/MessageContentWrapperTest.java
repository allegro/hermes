package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class MessageContentWrapperTest {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final DeserializationMetrics metrics = new DeserializationMetrics(metricRegistry);

    private final JsonMessageContentWrapper jsonWrapper = new JsonMessageContentWrapper("message", "metadata", new ObjectMapper());
    private final AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());

    private final AvroMessageAnySchemaVersionContentWrapper anySchemaWrapper =
            new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, () -> true, avroWrapper, metrics);
    private final AvroMessageSchemaVersionAwareContentWrapper schemaAwareWrapper =
            new AvroMessageSchemaVersionAwareContentWrapper(schemaRepository, avroWrapper, anySchemaWrapper, metrics);
    private final AvroMessageHeaderSchemaVersionContentWrapper headerSchemaWrapper = new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, schemaAwareWrapper, metrics);

    private final MessageContentWrapper messageContentWrapper = new MessageContentWrapper(jsonWrapper, avroWrapper,
            headerSchemaWrapper);

    static CompiledSchema<Schema> schema1 = new CompiledSchema<>(load("/schema/user.avsc"), SchemaVersion.valueOf(1));
    static CompiledSchema<Schema> schema2 = new CompiledSchema<>(load("/schema/user_v2.avsc"), SchemaVersion.valueOf(2));
    static CompiledSchema<Schema> schema3 = new CompiledSchema<>(load("/schema/user_v3.avsc"), SchemaVersion.valueOf(3));

    static SchemaVersionsRepository schemaVersionsRepository = new SchemaVersionsRepository() {
        @Override
        public List<SchemaVersion> versions(Topic topic, boolean online) {
            return online ? asList(schema3.getVersion(), schema2.getVersion(), schema1.getVersion())
                    : asList(schema2.getVersion(), schema1.getVersion());
        }

        @Override
        public void close() {
        }
    };

    static CompiledSchemaRepository<Schema> compiledSchemaRepository = (topic, version, online) -> {
        switch (version.value()) {
            case 1:
                return schema1;
            case 2:
                return schema2;
            case 3:
                return schema3;
            default:
                throw new RuntimeException("sry");
        }
    };
    static SchemaRepository schemaRepository = new SchemaRepository(schemaVersionsRepository, compiledSchemaRepository);

    @Before
    public void clean() {
        metricRegistry.getCounters().forEach((s, counter) -> counter.dec(counter.getCount()));
    }

    @Test
    public void shouldUnwrapMessageUsingEverySchemaAvailable() {
        // forcing offline latest
        shouldUnwrapMessageWrappedWithSchemaAtVersion(2, false, false, 0, 0, 0, 0);

        // forcing fallback
        shouldUnwrapMessageWrappedWithSchemaAtVersion(1, false, false, 0, 0, 0, 0);

        // forcing online check
        shouldUnwrapMessageWrappedWithSchemaAtVersion(3, false, false, 0, 0, 1, 0);
    }

    public void shouldUnwrapMessageWrappedWithSchemaAtVersion(int schemaVersion, boolean wrapWithSchemaVersionAwarePayload,
                                                              boolean unwrapWithSchemaVersionAwarePayload, int missedSchemaVersionInPayload,
                                                              int errorsForPayloadWithSchemaVersion, int errorsForAnySchemaVersion,
                                                              int errorsForAnyOnlineSchemaVersion) {
        shouldUnwrapMessageWrappedWithSchemaAtVersion(messageContentWrapper, schemaVersion, wrapWithSchemaVersionAwarePayload,
                unwrapWithSchemaVersionAwarePayload, missedSchemaVersionInPayload, errorsForPayloadWithSchemaVersion,
                errorsForAnySchemaVersion, errorsForAnyOnlineSchemaVersion);
    }

    public void shouldUnwrapMessageWrappedWithSchemaAtVersion(MessageContentWrapper wrapper,
                                                              int schemaVersion,
                                                              boolean wrapWithSchemaVersionAwarePayload,
                                                              boolean unwrapWithSchemaVersionAwarePayload,
                                                              int missedSchemaVersionInPayload,
                                                              int errorsForPayloadWithSchemaVersion,
                                                              int errorsForAnySchemaVersion,
                                                              int errorsForAnyOnlineSchemaVersion) {
        // given
        SchemaVersion version = SchemaVersion.valueOf(schemaVersion);
        Integer TODO = 1;
        Topic topicToWrap = createTopic("group", "topic", wrapWithSchemaVersionAwarePayload);
        Topic topicToUnwrap = createTopic("group", "topic", unwrapWithSchemaVersionAwarePayload);
        CompiledSchema<Schema> schema = schemaRepository.getKnownAvroSchemaVersion(topicToWrap, version);
        AvroUser user = new AvroUser(schema, "Bob", 15, "blue");
        byte[] wrapped = wrapper.wrapAvro(user.asBytes(), "uniqueId", 1234, topicToWrap, schema, new HashedMap<>());

        // when
        UnwrappedMessageContent unwrappedMessageContent = wrapper.unwrapAvro(wrapped, topicToUnwrap, TODO);

        // then
        assertThat(unwrappedMessageContent.getContent()).contains(user.asBytes());
        assertThat(unwrappedMessageContent.getSchema().get().getVersion()).isEqualTo(version);
        assertThat(unwrappedMessageContent.getMessageMetadata().getId()).isEqualTo("uniqueId");
        assertThat(unwrappedMessageContent.getMessageMetadata().getTimestamp()).isEqualTo(1234);

        assertThat(metrics.missedSchemaVersionInPayload().getCount()).isEqualTo(missedSchemaVersionInPayload);
        assertThat(metrics.errorsForSchemaVersionAwarePayload().getCount()).isEqualTo(errorsForPayloadWithSchemaVersion);
        assertThat(metrics.errorsForAnySchemaVersion().getCount()).isEqualTo(errorsForAnySchemaVersion);
        assertThat(metrics.errorsForAnyOnlineSchemaVersion().getCount()).isEqualTo(errorsForAnyOnlineSchemaVersion);
    }

    @Test
    public void shouldThrowExceptionWhenMessageCouldNotBeUnwrappedByAnySchema() {
        // given
        Topic topic = topic("group", "topic").build();
        Integer TODO = 1;
        byte[] doesNotMatchAnySchema = "{}".getBytes();

        // when
        catchException(messageContentWrapper).unwrapAvro(doesNotMatchAnySchema, topic, TODO);

        // then
        assertThat(caughtException() instanceof SchemaMissingException).isTrue();
        assertThat(metrics.errorsForAnySchemaVersion().getCount()).isEqualTo(1);
        assertThat(metrics.errorsForAnyOnlineSchemaVersion().getCount()).isEqualTo(1);
    }

    @Test
    public void shouldUnwrapMessageUsingSchemaVersionFromPayload() {
        shouldUnwrapMessageWrappedWithSchemaAtVersion(2, true, true, 0, 0, 0, 0);
    }

    @Test
    public void shouldFallbackToDeserializationWithAnySchemaWhenErrorOccursInSchemaVersionAwarePayloadDeserialization() {
        shouldUnwrapMessageWrappedWithSchemaAtVersion(3, false, true, 0, 1, 1, 0);
    }

    @Test
    public void shouldFallbackToAnySchemaDeserializationWhenSchemaVersionIsMissingInPayload() {
        shouldUnwrapMessageWrappedWithSchemaAtVersion(2, false, true, 1, 0, 0, 0);
    }

    private Topic createTopic(String group, String name, boolean schemaVersionAwarePayload) {
        return schemaVersionAwarePayload ? topic(group, name).withSchemaVersionAwareSerialization().build() : topic(group, name).build();
    }

    @Test
    public void shouldUseRateLimiterWhenTryingToOnlineCheckForSchemaVersions() throws IOException {
        // given
        SchemaOnlineChecksRateLimiter rateLimiter = mock(SchemaOnlineChecksRateLimiter.class);
        when(rateLimiter.tryAcquireOnlineCheckPermit()).thenReturn(true);


        AvroMessageAnySchemaVersionContentWrapper anySchemaWrapper =
                new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, rateLimiter, avroWrapper, metrics);
        AvroMessageSchemaVersionAwareContentWrapper schemaAwareWrapper =
                new AvroMessageSchemaVersionAwareContentWrapper(schemaRepository, avroWrapper, anySchemaWrapper, metrics);
        AvroMessageHeaderSchemaVersionContentWrapper headerSchemaWrapper = new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, schemaAwareWrapper, metrics);

        MessageContentWrapper wrapper = new MessageContentWrapper(jsonWrapper, avroWrapper, headerSchemaWrapper);

        // when forcing offline checks
        shouldUnwrapMessageWrappedWithSchemaAtVersion(wrapper, 1, false, false, 0, 0, 0, 0);
        shouldUnwrapMessageWrappedWithSchemaAtVersion(wrapper, 2, false, false, 0, 0, 0, 0);

        // then
        verify(rateLimiter, never()).tryAcquireOnlineCheckPermit();

        // when forcing online check
        shouldUnwrapMessageWrappedWithSchemaAtVersion(wrapper, 3, false, false, 0, 0, 1, 0);

        // then
        verify(rateLimiter).tryAcquireOnlineCheckPermit();
    }
}
