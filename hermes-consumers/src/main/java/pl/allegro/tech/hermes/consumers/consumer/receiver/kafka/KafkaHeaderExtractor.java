package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

import com.google.common.primitives.Ints;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker;
import pl.allegro.tech.hermes.consumers.config.KafkaHeaderNameProperties;

public class KafkaHeaderExtractor {

  private final KafkaHeaderNameProperties kafkaHeaderNameProperties;
  private final boolean isHTTPheadersPropagationAsKafkaHeadersEnabled;
  private final String httpHeadersPrefix;

  public KafkaHeaderExtractor(
      KafkaHeaderNameProperties kafkaHeaderNameProperties,
      HTTPHeadersPropagationAsKafkaHeadersProperties
          httpHeadersPropagationAsKafkaHeadersProperties) {

    this.kafkaHeaderNameProperties = kafkaHeaderNameProperties;
    this.isHTTPheadersPropagationAsKafkaHeadersEnabled =
        httpHeadersPropagationAsKafkaHeadersProperties.isEnabled();
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

  public Map<String, String> extractExternalMetadata(
      Headers headers, Map<String, String> defaultExternalMetadata) {
    if (isHTTPheadersPropagationAsKafkaHeadersEnabled) {
      Map<String, String> httpHeaders =
          stream(headers.spliterator(), false)
              .filter(h -> h.key().startsWith(httpHeadersPrefix))
              .collect(
                  toMap(
                      h -> h.key().substring(httpHeadersPrefix.length()),
                      h -> new String(h.value(), UTF_8)));
      if (httpHeaders.isEmpty()) {
        // After completing the migration to the approach with Kafka headers, we should remove this
        // condition.
        return defaultExternalMetadata;
      }
      // The following is necessary to be compatible with building external metadata based on the
      // message body.
      // See: pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper.getMetadata
      Map<String, String> externalMetadata = new HashMap<>(httpHeaders);
      externalMetadata.put(
          AvroMetadataMarker.METADATA_MESSAGE_ID_KEY.toString(), extractMessageId(headers));
      return externalMetadata;
    }
    return defaultExternalMetadata;
  }
}
