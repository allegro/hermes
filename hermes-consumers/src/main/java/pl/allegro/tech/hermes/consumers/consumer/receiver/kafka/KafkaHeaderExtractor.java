package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KafkaHeaderExtractor {

    private final String schemaVersionHeaderName;
    private final String schemaIdHeaderName;
    private final String messageIdHeaderName;
    private final String timestampHeaderName;

    public KafkaHeaderExtractor(String schemaVersionHeaderName, String schemaIdHeaderName, String messageIdHeaderName,
                                String timestampHeaderName) {
        this.schemaVersionHeaderName = schemaVersionHeaderName;
        this.schemaIdHeaderName = schemaIdHeaderName;
        this.messageIdHeaderName = messageIdHeaderName;
        this.timestampHeaderName = timestampHeaderName;
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
    public String extractMessageId(Headers headers) {
        Header header = headers.lastHeader(messageIdHeaderName);
        if (header == null) {
            return "";
        }
        return new String(header.value(), UTF_8);
    }

    public long extractTimestamp(Headers headers) {
        Header header = headers.lastHeader(timestampHeaderName);
        if (header == null) {
            return 0L;
        }
        return Longs.fromByteArray(header.value());
    }

}
