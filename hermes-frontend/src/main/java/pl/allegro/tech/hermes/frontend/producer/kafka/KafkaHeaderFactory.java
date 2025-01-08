package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.primitives.Ints;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaHeaderNameParameters;

public class KafkaHeaderFactory {

  private final KafkaHeaderNameParameters kafkaHeaderNameParameters;
  private final boolean isHTTPHeadersPropagationAsKafkaHeadersEnabled;
  private final String httpHeaderPrefix;

  public KafkaHeaderFactory(
      KafkaHeaderNameParameters kafkaHeaderNameParameters,
      HTTPHeadersPropagationAsKafkaHeadersProperties
          httpHeadersPropagationAsKafkaHeadersProperties) {
    this.kafkaHeaderNameParameters = kafkaHeaderNameParameters;
    this.isHTTPHeadersPropagationAsKafkaHeadersEnabled =
        httpHeadersPropagationAsKafkaHeadersProperties.isEnabled();
    this.httpHeaderPrefix = httpHeadersPropagationAsKafkaHeadersProperties.getPrefix();
  }

  Header messageId(String messageId) {
    return new RecordHeader(kafkaHeaderNameParameters.getMessageId(), messageId.getBytes());
  }

  Header schemaVersion(int schemaVersion) {
    return new RecordHeader(
        kafkaHeaderNameParameters.getSchemaVersion(), Ints.toByteArray(schemaVersion));
  }

  Header schemaId(int schemaId) {
    return new RecordHeader(kafkaHeaderNameParameters.getSchemaId(), Ints.toByteArray(schemaId));
  }

  void setHTTPHeadersIfEnabled(List<Header> headers, Map<String, String> httpHeaders) {
    if (isHTTPHeadersPropagationAsKafkaHeadersEnabled) {
      httpHeaders.forEach((name, value) -> headers.add(createHttpHeader(name, value)));
    }
  }

  private Header createHttpHeader(String name, String value) {
    return new RecordHeader(httpHeaderPrefix + name, value.getBytes(StandardCharsets.UTF_8));
  }
}
