package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.primitives.Ints;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.Optional;

public class KafkaHeaderExtractor {

    private final String schemaVersionHeaderName;
    private final String schemaIdHeaderName;

    @Inject
    public KafkaHeaderExtractor(ConfigFactory configFactory) {
        this.schemaVersionHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_VERSION);
        this.schemaIdHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_ID);
    }

    public Integer extractSchemaVersion(Headers headers) {
        Header header = headers.lastHeader(schemaVersionHeaderName);
        return extract(header);
    }

    public Integer extractSchemaId(Headers headers) {
        Header header = headers.lastHeader(schemaIdHeaderName);
        return extract(header);
    }

    private Integer extract(Header header) {
        if (header != null) {
            return Ints.fromByteArray(header.value());
        } else {
            return null;
        }
    }
}
