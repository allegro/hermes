package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.primitives.Ints;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;
import pl.allegro.tech.hermes.consumers.config.KafkaHeaderNameProperties;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class KafkaHeaderExtractor {

    private final KafkaHeaderNameProperties kafkaHeaderNameProperties;
    private final boolean isHTTPheadersPropagationAsKafkaHeadersEnabled;
    private final String httpHeadersPrefix;

    public KafkaHeaderExtractor(KafkaHeaderNameProperties kafkaHeaderNameProperties,
                                HTTPHeadersPropagationAsKafkaHeadersProperties httpHeadersPropagationAsKafkaHeadersProperties) {

        this.kafkaHeaderNameProperties = kafkaHeaderNameProperties;
        this.isHTTPheadersPropagationAsKafkaHeadersEnabled = httpHeadersPropagationAsKafkaHeadersProperties.isEnabled();
        this.httpHeadersPrefix = httpHeadersPropagationAsKafkaHeadersProperties.getPrefix();
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

    public Map<String, String> extractHTTPHeadersIfEnabled(Headers headers) {
        return isHTTPheadersPropagationAsKafkaHeadersEnabled
            ?
            stream(headers.spliterator(), false)
            .filter(h -> h.key().startsWith(httpHeadersPrefix))
            .collect(toMap(
                h -> h.key().substring(httpHeadersPrefix.length()),
                h -> new String(h.value(), UTF_8)))
            :
            emptyMap();
    }
}
