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

import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final DeserializationMetrics metrics = new DeserializationMetrics(metricRegistry);
    private final JsonMessageContentWrapper jsonWrapper = new JsonMessageContentWrapper("message", "metadata", new ObjectMapper());
    private final AvroMessageContentWrapper avroWrapper = new AvroMessageContentWrapper(Clock.systemDefaultZone());


    private final AvroMessageHeaderSchemaVersionContentWrapper headerSchemaVersionWrapper =
            new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroWrapper, metrics);
    private final AvroMessageSchemaIdAwareContentWrapper schemaAwareWrapper =
            new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroWrapper, metrics);

    private static final CompiledSchema<Schema> schema1 = CompiledSchema.of(load("/schema/user.avsc"), ID_ONE, VERSION_ONE);
    private static final CompiledSchema<Schema> schema2 = CompiledSchema.of(load("/schema/user_v2.avsc"), ID_THREE, VERSION_TWO);
    private static final CompiledSchema<Schema> schema3 = CompiledSchema.of(load("/schema/user_v3.avsc"), ID_FIVE, VERSION_THREE);

    private CompositeMessageContentWrapper createMessageContentWrapper(boolean schemaHeaderIdEnabled, boolean schemaVersionTruncationEnabled) {

        final AvroMessageHeaderSchemaIdContentWrapper headerSchemaIdWrapper =
            new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroWrapper, metrics, schemaHeaderIdEnabled);

        final AvroMessageSchemaVersionTruncationContentWrapper schemaIdAndHeaderContentWrapper =
                new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroWrapper, metrics, schemaVersionTruncationEnabled);

        return new CompositeMessageContentWrapper(jsonWrapper, avroWrapper, schemaAwareWrapper,
            headerSchemaVersionWrapper, headerSchemaIdWrapper, schemaIdAndHeaderContentWrapper);
    }

    private final CompositeMessageContentWrapper compositeMessageContentWrapper = createMessageContentWrapper(false, true);

    static SchemaVersionsRepository schemaVersionsRepository = new SchemaVersionsRepository() {
        @Override
        public SchemaVersionsResult versions(Topic topic, boolean online) {
            List<SchemaVersion> onlineVersions = asList(schema3.getVersion(), schema2.getVersion(), schema1.getVersion());
            List<SchemaVersion> cachedVersions = asList(schema2.getVersion(), schema1.getVersion());
            return online
                    ? SchemaVersionsResult.succeeded(onlineVersions)
                    : SchemaVersionsResult.succeeded(cachedVersions);
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
        public CompiledSchema<Schema> getSchema(Topic topic, SchemaId id) {
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
    public void shouldUnwrapMessageUsingSchemaIdFromPayload() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_FIVE);
        Topic topic = createTopicWithSchemaIdAwarePayload();
        AvroUser user = createAvroUser(schemaId, topic);

        byte[] wrapped =
                compositeMessageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = compositeMessageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 1, 0, 0, 0);
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
                compositeMessageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = compositeMessageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, schemaVersion.value());

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldUnwrapUsingHeaderSchemaIdIfHeaderPresent() {
        // given
        CompositeMessageContentWrapper compositeMessageContentWrapperWithHeaderEnabled = createMessageContentWrapper(true, false);
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaId schemaId = createSchemaId(ID_THREE);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaId, topic);

        byte[] wrapped = compositeMessageContentWrapperWithHeaderEnabled
            .wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent = compositeMessageContentWrapperWithHeaderEnabled
            .unwrapAvro(wrapped, topic, schemaId.value(), NO_VERSION_IN_HEADER);

        // then
        assertResult(unwrappedMessageContent, schemaId, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 1, 0);
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
                compositeMessageContentWrapper.wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, schema, NO_EXTERNAL_METADATA);

        // when
        UnwrappedMessageContent unwrappedMessageContent =
            compositeMessageContentWrapper.unwrapAvro(wrapped, topic, schema.getId().value(), schema.getVersion().value());

        // then
        assertResult(unwrappedMessageContent, schema.getVersion(), user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 1, 0, 0, 0);
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
                compositeMessageContentWrapper
                        .wrapAvro(user.asBytes(), messageId, messageTimestamp, topicToWrap, user.getCompiledSchema(), NO_EXTERNAL_METADATA);

        Topic topicToUnwrap = createTopicWithSchemaIdAwarePayload();

        // when
        UnwrappedMessageContent unwrappedMessageContent = compositeMessageContentWrapper.unwrapAvro(wrapped, topicToUnwrap, NO_ID_IN_HEADER, schemaVersion.value());

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);

        // missedSchemaVersionInPayload == no magic byte
        assertMetrics(1, 0, 0, 0, 0, 0, 1, 0, 0);
    }

    @Test
    public void shouldTrimSchemaVersionFromMessageWhenSchemaVersionPresentInHeader() {
        // given
        String messageId = MESSAGE_ID;
        int messageTimestamp = MESSAGE_TIMESTAMP;

        SchemaVersion schemaVersion = createSchemaVersion(VERSION_ONE);
        Topic topic = createTopic();
        AvroUser user = createAvroUser(schemaVersion, topic);

        byte[] message =
                compositeMessageContentWrapper
                        .wrapAvro(user.asBytes(), messageId, messageTimestamp, topic, user.getCompiledSchema(), NO_EXTERNAL_METADATA);
        byte[] wrapped = serializeWithSchemaVersionInPayload(schemaVersion, message);

        // when
        UnwrappedMessageContent unwrappedMessageContent = compositeMessageContentWrapper.unwrapAvro(wrapped, topic, NO_ID_IN_HEADER, VERSION_ONE);

        // then
        assertResult(unwrappedMessageContent, schemaVersion, user.asBytes(), messageId, messageTimestamp);
        assertMetrics(0, 0, 0, 0, 0, 0, 0, 0, 1);
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
                               int errorsForHeaderSchemaVersion,
                               int errorsForHeaderSchemaId,
                               int errorsWithSchemaVersionTruncation,
                               int usingSchemaIdAware,
                               int usingHeaderSchemaVersion,
                               int usingHeaderSchemaId,
                               int usingSchemaVersionTruncation) {
        assertThat(metrics.missedSchemaIdInPayload().getCount()).isEqualTo(missedSchemaIdInPayload);
        assertThat(metrics.errorsForSchemaIdAwarePayload().getCount()).isEqualTo(errorsForPayloadWithSchemaId);
        assertThat(metrics.errorsForHeaderSchemaVersion().getCount()).isEqualTo(errorsForHeaderSchemaVersion);
        assertThat(metrics.errorsForHeaderSchemaId().getCount()).isEqualTo(errorsForHeaderSchemaId);
        assertThat(metrics.errorsForSchemaVersionTruncation().getCount()).isEqualTo(errorsWithSchemaVersionTruncation);
        assertThat(metrics.usingSchemaIdAware().getCount()).isEqualTo(usingSchemaIdAware);
        assertThat(metrics.usingHeaderSchemaVersion().getCount()).isEqualTo(usingHeaderSchemaVersion);
        assertThat(metrics.usingHeaderSchemaId().getCount()).isEqualTo(usingHeaderSchemaId);
        assertThat(metrics.usingSchemaVersionTruncation().getCount()).isEqualTo(usingSchemaVersionTruncation);
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

    private static byte[] serializeWithSchemaVersionInPayload(SchemaVersion schemaVersion, byte[] data) {
        final int HEADER_SIZE = 5;
        final byte MAGIC_BYTE_VALUE = 0;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + data.length);
        buffer.put(MAGIC_BYTE_VALUE);
        buffer.putInt(schemaVersion.value());
        buffer.put(data);
        return buffer.array();
    }
}
