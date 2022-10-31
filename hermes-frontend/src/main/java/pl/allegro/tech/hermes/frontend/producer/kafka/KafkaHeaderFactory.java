package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

import java.nio.charset.StandardCharsets;

public class KafkaHeaderFactory {

    private final KafkaHeaderNameParameters kafkaHeaderNameParameters;

    public KafkaHeaderFactory(KafkaHeaderNameParameters kafkaHeaderNameParameters) {
        this.kafkaHeaderNameParameters = kafkaHeaderNameParameters;
    }

    Header messageId(String messageId) {
        return new RecordHeader(kafkaHeaderNameParameters.getMessageId(), messageId.getBytes());
    }

    Header timestamp(long timestamp) {
        return new RecordHeader(kafkaHeaderNameParameters.getTimestamp(), Longs.toByteArray(timestamp));
    }

    Header schemaVersion(int schemaVersion) {
        return new RecordHeader(kafkaHeaderNameParameters.getSchemaVersion(), Ints.toByteArray(schemaVersion));
    }

    Header schemaId(int schemaId) {
        return new RecordHeader(kafkaHeaderNameParameters.getSchemaId(), Ints.toByteArray(schemaId));
    }

    Header httpHeader(String name, String value) {
        return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
