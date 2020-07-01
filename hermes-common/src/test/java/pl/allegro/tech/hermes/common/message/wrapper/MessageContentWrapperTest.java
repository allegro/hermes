package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.schema.*;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.time.Clock;
import java.util.List;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader.load;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class MessageContentWrapperTest {

    private static final Integer NO_VERSION_IN_HEADER = null;
    private static final Integer NO_ID_IN_HEADER = null;
    private static final int MESSAGE_TIMESTAMP = 1582029457;
    private static final HashedMap<String, String> NO_EXTERNAL_METADATA = new HashedMap<>();
    private static final String MESSAGE_ID = "messageId-1";
    private static final int VERSION_ONE = 1;
    private static final int VERSION_TWO = 2;
    private static final int VERSION_THREE = 3;
    private static final int ID_ONE = 1;
    private static final int ID_THREE = 3;
    private static final int ID_FIVE = 5;
    private static final String EMPTY_SCHEMA = "{}";

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final DeserializationMetrics metrics = new DeserializationMetrics(metricRegistry);
    private final JsonMessageContentWrapper jsonWrapper = new JsonMessageContentWrapper("message", "metadata", new ObjectMapper());
    private final AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());

    private final SchemaOnlineChecksRateLimiter rateLimiter = mock(SchemaOnlineChecksRateLimiter.class);

    private final AvroMessageAnySchemaVersionContentWrapper anySchemaWrapper =
            new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, rateLimiter, avroWrapper, metrics);
    private final AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionWrapper =
            new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, metrics);
    private final AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdWrapper =
        new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroWrapper, metrics);
    private final AvroMessageSchemaIdAwareContentWrapper schemaAwareWrapper =
            new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroWrapper, metrics);

    private final MessageContentWrapper messageContentWrapper = new MessageContentWrapper(jsonWrapper, avroWrapper, schemaAwareWrapper,
        headerSchemaVersionWrapper, headerSchemaIdWrapper, anySchemaWrapper);

    private static CompiledSchema<Schema> schema1 = CompiledSchema.of(load("/schema/user.avsc"), ID_ONE, VERSION_ONE);
    private static CompiledSchema<Schema> schema2 = CompiledSchema.of(load("/schema/user_v2.avsc"), ID_THREE, VERSION_TWO);
    private static CompiledSchema<Schema> schema3 = CompiledSchema.of(load("/schema/user_v3.avsc"), ID_FIVE, VERSION_THREE);

    static SchemaVersionsRepository schemaVersionsRepository = new SchemaVersionsRepository() {
        @Override
        public List<SchemaVersion> versions(Topic topic, boolean online) {
            return online ?
                    asList(schema3.getVersion(), schema2.getVersion(), schema1.getVersion())
                    : asList(schema2.getVersion(), schema1.getVersion());
        }

        @Override
        public void close() {
        }
    };

    static CompiledSchemaRepository<Schema> compiledSchemaRepository = new CompiledSchemaRepository<Schema>() {
        @Override
        public CompiledSchema<Schema> getSchema(Topic topic, SchemaVersion version, boolean online) {
            switch (version.value()) {
                case VERSION_ONE:
                    return schema1;
                case VERSION_TWO:
                    return schema2;
                case VERSION_THREE:
                    return schema3;
                default:
                    throw new RuntimeException("sry");
            }
        }

        @Override
        public CompiledSchema<Schema> getSchema(Topic topic, SchemaId id, boolean online) {
            switch (id.value()) {
                case ID_ONE:
                    return schema1;
                case ID_THREE:
                    return schema2;
                case ID_FIVE:
                    return schema3;
                default:
                    throw new RuntimeException("sry");
            }
        }
    };

    static SchemaRepository schemaRepository = new SchemaRepository(schemaVersionsRepository, compiledSchemaRepository);

    @Before
    public void clean() {
        metricRegistry.getCounters().forEach((s, counter) -> counter.dec(counter.getCount()));
    }

    @Test
    public void shouldUnwrapMessageUsingAnySchemaWrapperForcingFallback() {
        // (v1, v2) locally given, asking for v1, should try v2 first and fallback to v1

        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_ONE);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldUnwrapUsingAnySchemaForcingOfflineLatest() {
        // (v1, v2) locally given, asking for v2, should try v2 first and succeed

        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_TWO);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldUnwrapMessageUsingAnySchemaForcingOnlineCheck() {
        // (v1, v2) locally and (v1, v2, v3) remotely given, asking for v3, should try v1/v2 locally first, fail, fallback to remote (online), then succeed

        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_THREE);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        when(rateLimiter.tryAcquireOnlineCheckPermit()).thenReturn(true);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 1, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldThrowExceptionWhenMessageCouldNotBeUnwrappedByAnySchema() {
        // given
        Topic topic = createTopic();
        byte[] doesNotMatchAnySchema = EMPTY_SCHEMA.getBytes();

        when(rateLimiter.tryAcquireOnlineCheckPermit()).thenReturn(true);

        // when
        catchException(messageContentWrapper).unwrapAvro(doesNotMatchAnySchema, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertThat(caughtException() instanceof SchemaMissingException).isTrue();
        assertMetrics(0, 0, 1, 1, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldUnwrapMessageUsingSchemaIdFromPayload() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_FIVE);
        Topic topic = createTopicWithSchemaIdAwarePayload();
        AvroUser user = createAvroUser(schemaId, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 1, 0, 0, 0);
    }

    @Test
    public void shouldFallbackToDeserializationWithAnySchemaWhenErrorOccursInSchemaIdAwarePayloadDeserialization() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_FIVE);

        // starts with magic byte, will be handled by schemaVersionAwareWrapper
        Topic topicToWrap = createTopic();
        AvroUser user = createAvroUser(schemaId, topicToWrap);

        byte[] wrapped = messageContentWrapper
                .wrapAvro(user.asBytes(), messageId, messageTimestamp, topicToWrap, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        Topic topicToUnwrap = createTopicWithSchemaIdAwarePayload(); // simulating exception by unwrapping with different topic

        when(rateLimiter.tryAcquireOnlineCheckPermit()).thenReturn(true);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topicToUnwrap, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 1, 1, 0, 0, 0, 1, 1, 0, 0);
    }

    @Test
    public void shouldFallbackToAnySchemaDeserializationWhenSchemaIdIsMissingInPayload() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        // does not start with magic byte, will not be handled by schemaVersionAwareWrapper
        SchemaId schemaId = createSchemaId(VERSION_THREE);
        Topic topicToWrap = createTopic();
        AvroUser user = createAvroUser(schemaId, topicToWrap);

        byte[] wrapped = messageContentWrapper
                .wrapAvro(user.asBytes(), messageId, messageTimestamp, topicToWrap, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        Topic topicToUnwrap = createTopicWithSchemaIdAwarePayload(); // simulating exception by unwrapping with different topic

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topicToUnwrap, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);

        // missedSchemaVersionInPayload == no magic byte
        assertMetrics(1, 0, 0, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldUseRateLimiterWhenTryingToOnlineCheckForSchemaVersions() {
        // given
        when(rateLimiter.tryAcquireOnlineCheckPermit()).thenReturn(true);

        // when (no online check with local fallback)
        unwrapWithAnyVersionWrapper(createSchemaVersion(VERSION_ONE));

        // then
        verify(rateLimiter, never()).tryAcquireOnlineCheckPermit();

        // when (no online check with no local fallback)
        unwrapWithAnyVersionWrapper(createSchemaVersion(VERSION_TWO));

        // then
        verify(rateLimiter, never()).tryAcquireOnlineCheckPermit();

        // when (with online check after local fallbacks fail)
        unwrapWithAnyVersionWrapper(createSchemaVersion(VERSION_THREE));
        verify(rateLimiter, times(1)).tryAcquireOnlineCheckPermit();
    }

    @Test
    public void shouldUnwrapUsingHeaderSchemaVersionIfHeaderPresent() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_TWO);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, schemaVersion.value());

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void shouldUnwrapUsingHeaderSchemaIdIfHeaderPresent() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_THREE);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaId, topic);

        byte[] wrapped =
            messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, schemaId.value(), NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 1);
    }

    @Test
    public void shouldUnwrapUsingSchemaIdAwareIfVersionAndIdInSchemaPresentDespiteServiceHeaderPresent() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_THREE);
        Topic topic = createTopicWithSchemaIdAwarePayload();
        AvroUser user = createAvroUser(schemaId, topic);
        CompiledSchema<Schema> schema = user.getCompiledSchema();

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, schema, NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent =
            messageContentWrapper.unwrapAvro(wrapped, topic, schema.getId().value(), schema.getVersion().value());

        // then
        assertResult(unwrappedMessageContent, schema.getVersion(), user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 1, 0, 0, 0);
    }

    @Test
    public void shouldUnwrapUsingHeaderSchemaVersionIfHeaderPresentAndNoMagicByte() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_TWO); // no magic byte
        Topic topicToWrap = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topicToWrap);

        byte[] wrapped =
                messageContentWrapper
                        .wrapAvro(user.asBytes(), messageId, messageTimestamp, topicToWrap, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        Topic topicToUnwrap = createTopicWithSchemaIdAwarePayload();

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topicToUnwrap, NO_ID_IN_HEADER, schemaVersion.value());

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);

        // missedSchemaVersionInPayload == no magic byte
        assertMetrics(1, 0, 0, 0, 0, 0, 0, 0, 1, 0);
    }

    @Test
    public void shouldUnwrapWithHeaderSchemaVersionWithFallbackToAnySchemaVersion() {
        // given
        final int NON_EXISTING_SCHEMA_VERSION = -1;

        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_TWO);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper
                        .wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NON_EXISTING_SCHEMA_VERSION);

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 1, 0, 0, 1, 1, 0);
    }

    private void unwrapWithAnyVersionWrapper(SchemaVersion schemaVersion) {
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] wrapped =
                messageContentWrapper.wrapAvro(user.asBytes(), MESSAGE_ID, MESSAGE_TIMESTAMP, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);
        messageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);
    }

    private void assertResult(UnwrappedMessageContent result,
                              SchemaVersion schemaVersion,
                              byte[] recordBytes,
                              String messageId,
                              int timestamp) {
        assertThat(result.getSchema().get().getVersion()).isEqualTo(schemaVersion);
        assertResult(result, recordBytes, messageId, timestamp);
    }

    private void assertResult(UnwrappedMessageContent result,
                              SchemaId schemaId,
                              byte[] recordBytes,
                              String messageId,
                              int timestamp) {
        assertThat(result.getSchema().get().getId()).isEqualTo(schemaId);
        assertResult(result, recordBytes, messageId, timestamp);
    }

    private void assertResult(UnwrappedMessageContent result, byte[] recordBytes, String messageId, int timestamp) {
        assertThat(result.getContent()).contains(recordBytes);
        assertThat(result.getMessageMetadata().getId()).isEqualTo(messageId);
        assertThat(result.getMessageMetadata().getTimestamp()).isEqualTo(timestamp);
    }

    private void assertMetrics(int missedSchemaIdInPayload,
                               int errorsForPayloadWithSchemaId,
                               int errorsForAnySchemaVersion,
                               int errorsForAnyOnlineSchemaVersion,
                               int errorsForHeaderSchemaVersion,
                               int errorsForHeaderSchemaId,
                               int usingSchemaIdAware,
                               int usingAnySchemaVersion,
                               int usingHeaderSchemaVersion,
                               int usingHeaderSchemaId) {
        assertThat(metrics.missedSchemaIdInPayload().getCount()).isEqualTo(missedSchemaIdInPayload);
        assertThat(metrics.errorsForSchemaIdAwarePayload().getCount()).isEqualTo(errorsForPayloadWithSchemaId);
        assertThat(metrics.errorsForAnySchemaVersion().getCount()).isEqualTo(errorsForAnySchemaVersion);
        assertThat(metrics.errorsForAnyOnlineSchemaVersion().getCount()).isEqualTo(errorsForAnyOnlineSchemaVersion);
        assertThat(metrics.errorsForHeaderSchemaVersion().getCount()).isEqualTo(errorsForHeaderSchemaVersion);
        assertThat(metrics.errorsForHeaderSchemaId().getCount()).isEqualTo(errorsForHeaderSchemaId);
        assertThat(metrics.usingSchemaIdAware().getCount()).isEqualTo(usingSchemaIdAware);
        assertThat(metrics.usingAnySchemaVersion().getCount()).isEqualTo(usingAnySchemaVersion);
        assertThat(metrics.usingHeaderSchemaVersion().getCount()).isEqualTo(usingHeaderSchemaVersion);
        assertThat(metrics.usingHeaderSchemaId().getCount()).isEqualTo(usingHeaderSchemaId);
    }

    private Topic createTopic() {
        return topic("group", "topic").build();
    }

    private Topic createTopicWithSchemaIdAwarePayload() {
        return topic("group", "topic-idAware").withSchemaIdAwareSerialization().build();
    }

    private AvroUser createAvroUser(SchemaVersion schemaVersion, Topic topic) {
        CompiledSchema<Schema> schema = schemaRepository.getKnownAvroSchemaVersion(topic, schemaVersion);
        return new AvroUser(schema, "user-1", 15, "colour-1");
    }

    private AvroUser createAvroUser(SchemaId id, Topic topic) {
        CompiledSchema<Schema> schema = schemaRepository.getAvroSchema(topic, id);
        return new AvroUser(schema, "user-1", 15, "colour-1");
    }

    private static SchemaVersion createSchemaVersion(int schemaVersionValue) {
        return SchemaVersion.valueOf(schemaVersionValue);
    }

    private static SchemaId createSchemaId(int schemaIdValue) {
        return SchemaId.valueOf(schemaIdValue);
    }
}
