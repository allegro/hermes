package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka

import com.codahale.metrics.MetricRegistry
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.io.EncoderFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.di.factories.ConfigFactoryCreator
import pl.allegro.tech.hermes.common.kafka.KafkaTopic
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
import pl.allegro.tech.hermes.common.message.wrapper.*
import pl.allegro.tech.hermes.schema.*
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class KafkaConsumerRecordToMessageConverterTest extends Specification {

    def schema = SchemaBuilder.record("FooRecord").fields()
            .name("avroRecordField").type().stringType().noDefault()
            .endRecord()

    def schemaId = SchemaId.valueOf(21)

    def schemaVersion = SchemaVersion.valueOf(1)

    def record = new GenericRecordBuilder(schema)
            .set("avroRecordField", "avroRecordFieldValue")
            .build()

    def clockUsedByAvroContentWrapper = Clock.fixed(Instant.ofEpochMilli(123), ZoneOffset.UTC)

    def clockUsedByConverter = Clock.fixed(Instant.ofEpochMilli(234), ZoneOffset.UTC)

    def "should convert avro record without metadata"() {
        given:
        def topic = topic("group.topic").build()
        def sub = subscription(topic, "sub1")
                .withHeader("subscriptionHeader", "subscriptionHeaderValue").build()
        BasicMessageContentReader contentReader = createContentReader(topic, false, false)
        KafkaConsumerRecordToMessageConverter converter = createKafkaMessageConverter(topic, sub, ContentType.AVRO, contentReader)

        byte[] data = serializeRecordInConfluentFormat()
        def kafkaRecordTimestamp = 345L
        def kafkaHeaders = new RecordHeaders(Arrays.asList(
                new RecordHeader("kafkaRecordHeader", "kafkaRecordValue".getBytes(StandardCharsets.UTF_8))))
        def kafkaRecord = new ConsumerRecord<byte[], byte[]>(topic.getQualifiedName(), 0, 456, kafkaRecordTimestamp, TimestampType.CREATE_TIME,
                ConsumerRecord.NULL_CHECKSUM.longValue(), ConsumerRecord.NULL_SIZE, ConsumerRecord.NULL_SIZE, new byte[0], data, kafkaHeaders)
        def partitionAssigmentTerm = 567L

        when:
        def message = converter.convertToMessage(kafkaRecord, partitionAssigmentTerm)

        then:
        message.getTopic() == topic.getQualifiedName()
        message.getSubscription() == sub.getName()
        message.getContentType() == ContentType.AVRO
        message.getSchema() == Optional.of(new CompiledSchema(schema, schemaId, schemaVersion))
        message.getPartitionOffset() == new PartitionOffset(KafkaTopicName.valueOf(topic.getQualifiedName()), kafkaRecord.offset(), kafkaRecord.partition())
        message.getPartitionAssignmentTerm() == partitionAssigmentTerm

        message.getId() == "" // default in case of missing __meta
        message.getData() == data
        message.getPublishingTimestamp() == clockUsedByAvroContentWrapper.instant().toEpochMilli() // default in case of missing __meta
        message.getReadingTimestamp() == clockUsedByConverter.instant().toEpochMilli()
        message.getExternalMetadata() == Collections.emptyMap()
        message.getAdditionalHeaders() == sub.getHeaders()
    }

    private BasicMessageContentReader createContentReader(Topic topic, Boolean schemaIdHeaderEnabled, Boolean magicByteTruncationEnabled) {
        def avroMessageContentWrapper = new AvroMessageContentWrapper(clockUsedByAvroContentWrapper)
        def schemaVersionRepository = new SchemaVersionsRepository() {
            @Override
            SchemaVersionsResult versions(Topic t, boolean online) {
                SchemaVersionsResult.succeeded(Collections.singletonList(schemaVersion));
            }
            @Override
            void close() {
            }
        }
        def compiledAvroSchemaRepository = new CompiledSchemaRepository<Schema>() {
            @Override
            CompiledSchema<Schema> getSchema(Topic t, SchemaVersion version, boolean online) {
                return new CompiledSchema(schema, schemaId, schemaVersion)
            }

            @Override
            CompiledSchema<Schema> getSchema(Topic t, SchemaId id) {
                return new CompiledSchema(schema, schemaId, schemaVersion)
            }
        }
        def schemaRepository = new SchemaRepository(schemaVersionRepository, compiledAvroSchemaRepository)
        def metrics = new DeserializationMetrics(new MetricRegistry())
        def wrapper = new CompositeMessageContentWrapper(null, avroMessageContentWrapper,
                new AvroMessageSchemaIdAwareContentWrapper(schemaRepository, avroMessageContentWrapper, metrics),
                new AvroMessageHeaderSchemaVersionContentWrapper(schemaRepository, avroMessageContentWrapper, metrics),
                new AvroMessageHeaderSchemaIdContentWrapper(schemaRepository, avroMessageContentWrapper, metrics, schemaIdHeaderEnabled),
                new AvroMessageAnySchemaVersionContentWrapper(schemaRepository, { -> true }, avroMessageContentWrapper, metrics),
                new AvroMessageSchemaVersionTruncationContentWrapper(schemaRepository, avroMessageContentWrapper, metrics, magicByteTruncationEnabled))
        def headerExtractor = new KafkaHeaderExtractor(new ConfigFactoryCreator().provide())
        new BasicMessageContentReader(wrapper, headerExtractor, topic)
    }

    private KafkaConsumerRecordToMessageConverter createKafkaMessageConverter(Topic topic, Subscription sub, ContentType contentType, BasicMessageContentReader contentReader) {
        def topics = Collections.singletonMap(
                topic.getQualifiedName(), new KafkaTopic(KafkaTopicName.valueOf(topic.getQualifiedName()), contentType))
        new KafkaConsumerRecordToMessageConverter(topic, sub, topics, contentReader, clockUsedByConverter)
    }

    private byte[] serializeRecordInConfluentFormat() {
        def bos = new ByteArrayOutputStream()
        def encoder = EncoderFactory.get().binaryEncoder(bos, null)
        GenericData.get().createDatumWriter(schema).write(record, encoder)
        encoder.flush()
        SchemaAwareSerDe.serialize(schemaId, bos.toByteArray())
    }

}
