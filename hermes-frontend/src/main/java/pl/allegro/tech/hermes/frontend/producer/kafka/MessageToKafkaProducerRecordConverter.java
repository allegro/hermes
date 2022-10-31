package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.collect.Lists;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.util.List;

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
        Iterable<Header> headers = createRecordHeaders(message);
        byte[] partitionKey = ofNullable(message.getPartitionKey()).map(String::getBytes).orElse(null);

        return new ProducerRecord<>(kafkaTopicName.asString(), null, message.getTimestamp(),
                partitionKey, message.getData(), headers);
    }

    private Iterable<Header> createRecordHeaders(Message message) {
        List<Header> headers = Lists.newArrayList(
                kafkaHeaderFactory.messageId(message.getId())
        );

        message.<Schema>getCompiledSchema().ifPresent(compiledSchemaVersion -> {
            headers.add(kafkaHeaderFactory.schemaVersion(compiledSchemaVersion.getVersion().value()));
            if (schemaIdHeaderEnabled) {
                headers.add(kafkaHeaderFactory.schemaId(compiledSchemaVersion.getId().value()));
            }
        });

        message.getHTTPHeaders().forEach((name, value) -> headers.add(kafkaHeaderFactory.httpHeader(name, value)));

        return headers;
    }

}
