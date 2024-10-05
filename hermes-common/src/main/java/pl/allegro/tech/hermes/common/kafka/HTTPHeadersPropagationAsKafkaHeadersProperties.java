package pl.allegro.tech.hermes.common.kafka;

public interface HTTPHeadersPropagationAsKafkaHeadersProperties {
  boolean isEnabled();

  String getPrefix();
}
