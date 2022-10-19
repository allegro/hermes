package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class MessageToKafkaProducerRecordConverter {

    private final KafkaHeaderFactory kafkaHeaderFactory;
    private final boolean schemaIdHeaderEnabled;

    public MessageToKafkaProducerRecordConverter(KafkaHeaderFactory kafkaHeaderFactory,
                                                 boolean schemaIdHeaderEnabled) {
        this.kafkaHeaderFactory = kafkaHeaderFactory;
        this.schemaIdHeaderEnabled = schemaIdHeaderEnabled;
    }

    public ProducerRecord<byte[], byte[]> convertToProducerRecord(Message message, KafkaTopicName kafkaTopicName) {
        Optional<SchemaVersion> schemaVersion = message.<Schema>getCompiledSchema().map(CompiledSchema::getVersion);
        Optional<SchemaId> schemaId = createSchemaId(message);
        Iterable<Header> headers = createRecordHeaders(message.getId(), message.getTimestamp(), schemaId, schemaVersion);
        byte[] partitionKey = ofNullable(message.getPartitionKey()).map(String::getBytes).orElse(null);

        return new ProducerRecord<>(kafkaTopicName.asString(), null, message.getTimestamp(),
                partitionKey, message.getData(), headers);
    }

    private Optional<SchemaId> createSchemaId(Message message) {
        if (schemaIdHeaderEnabled) {
            return message.<Schema>getCompiledSchema().map(CompiledSchema::getId);
        }

        return Optional.empty();
    }

    private Iterable<Header> createRecordHeaders(String id,
                                                 long timestamp,
                                                 Optional<SchemaId> schemaId,
                                                 Optional<SchemaVersion> schemaVersion) {
        Stream<Optional<Header>> headers = Stream.of(
                Optional.of(kafkaHeaderFactory.messageId(id)),
                Optional.of(kafkaHeaderFactory.timestamp(timestamp)),
                schemaVersion.map(sv -> kafkaHeaderFactory.schemaVersion(sv.value())),
                schemaId.map(sid -> kafkaHeaderFactory.schemaId(sid.value()))
        );

        return headers
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

}
