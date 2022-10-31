package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.primitives.Ints;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import pl.allegro.tech.hermes.consumers.config.KafkaHeaderNameProperties;

import java.util.Map;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

public class KafkaHeaderExtractor {

    private final KafkaHeaderNameProperties kafkaHeaderNameProperties;

    public KafkaHeaderExtractor(KafkaHeaderNameProperties kafkaHeaderNameProperties) {

        this.kafkaHeaderNameProperties = kafkaHeaderNameProperties;
    }

    public Integer extractSchemaVersion(Headers headers) {
        Header header = headers.lastHeader(kafkaHeaderNameProperties.getSchemaVersion());
        return extract(header);
    }

    public Integer extractSchemaId(Headers headers) {
        Header header = headers.lastHeader(kafkaHeaderNameProperties.getSchemaId());
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
        Header header = headers.lastHeader(kafkaHeaderNameProperties.getMessageId());
        if (header == null) {
            return "";
        }
        return new String(header.value(), UTF_8);
    }

    public Map<String, String> extractExternalMetadata(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false)
                .filter(h -> kafkaHeaderNameProperties.isNotInternal(h.key()))
                .collect(toMap(Header::key, h -> new String(h.value(), UTF_8)));
    }
}
