package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;

public class KafkaHeaderFactory {

    private final String messageIdHeaderName;
    private final String timestampHeaderName;
    private final String schemaVersionHeaderName;

    @Inject
    public KafkaHeaderFactory(ConfigFactory configFactory) {
        this.messageIdHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_MESSAGE_ID);
        this.timestampHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_TIMESTAMP);
        this.schemaVersionHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_VERSION);
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
}
