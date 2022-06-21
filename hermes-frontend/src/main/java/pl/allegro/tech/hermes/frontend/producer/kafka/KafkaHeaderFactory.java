package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

public class KafkaHeaderFactory {

    private final String messageIdHeaderName;
    private final String timestampHeaderName;
    private final String schemaVersionHeaderName;
    private final String schemaIdHeaderName;

    public KafkaHeaderFactory(String messageIdHeaderName, String timestampHeaderName, String schemaVersionHeaderName, String schemaIdHeaderName) {
        this.messageIdHeaderName = messageIdHeaderName;
        this.timestampHeaderName = timestampHeaderName;
        this.schemaVersionHeaderName = schemaVersionHeaderName;
        this.schemaIdHeaderName = schemaIdHeaderName;
    }

    Header messageId(String messageId) {
        return new RecordHeader(messageIdHeaderName, messageId.getBytes());
    }

    Header timestamp(long timestamp) {
        return new RecordHeader(timestampHeaderName, Longs.toByteArray(timestamp));
    }

    Header schemaVersion(int schemaVersion) {
        return new RecordHeader(schemaVersionHeaderName, Ints.toByteArray(schemaVersion));
    }

    Header schemaId(int schemaId) {
        return new RecordHeader(schemaIdHeaderName, Ints.toByteArray(schemaId));
    }
}
