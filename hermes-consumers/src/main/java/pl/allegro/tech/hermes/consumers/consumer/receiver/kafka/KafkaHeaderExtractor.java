package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.primitives.Ints;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

public class KafkaHeaderExtractor {

    private final String schemaVersionHeaderName;
    private final String schemaIdHeaderName;

    public KafkaHeaderExtractor(String schemaVersionHeaderName, String schemaIdHeaderName) {
        this.schemaVersionHeaderName = schemaVersionHeaderName;
        this.schemaIdHeaderName = schemaIdHeaderName;
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
