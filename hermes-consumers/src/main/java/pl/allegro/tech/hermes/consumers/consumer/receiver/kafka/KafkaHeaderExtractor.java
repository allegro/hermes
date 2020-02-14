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

    @Inject
    public KafkaHeaderExtractor(ConfigFactory configFactory) {
        this.schemaVersionHeaderName = configFactory.getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_VERSION);
    }

    public Optional<Integer> extractSchemaVersion(Headers headers) {
        Header header = headers.lastHeader(schemaVersionHeaderName);

        if (header != null) {
            return Optional.of(Ints.fromByteArray(header.value()));
        } else {
            return Optional.empty();
        }
    }
}
