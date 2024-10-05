package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;

@ConfigurationProperties(prefix = "consumer.http.headers.propagation-as-kafka-headers")
public class ConsumerHTTPHeadersPropagationAsKafkaHeadersProperties
    implements HTTPHeadersPropagationAsKafkaHeadersProperties {

  public boolean enabled = true;
  public String prefix = "h-";

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
}
