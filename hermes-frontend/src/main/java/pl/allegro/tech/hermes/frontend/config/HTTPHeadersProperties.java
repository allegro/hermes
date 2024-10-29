package pl.allegro.tech.hermes.frontend.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;

@ConfigurationProperties(prefix = "frontend.http.headers")
public class HTTPHeadersProperties {

  public boolean propagationEnabled = false;
  public Set<String> allowedSet = new HashSet<>();
  public Set<String> additionalAllowedSetToLog = new HashSet<>();
  public PropagationAsKafkaHeadersProperties propagationAsKafkaHeaders =
      new PropagationAsKafkaHeadersProperties();

  public static class PropagationAsKafkaHeadersProperties
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

  public boolean isPropagationEnabled() {
    return propagationEnabled;
  }

  public void setPropagationEnabled(boolean propagationEnabled) {
    this.propagationEnabled = propagationEnabled;
  }

  public PropagationAsKafkaHeadersProperties getPropagationAsKafkaHeaders() {
    return propagationAsKafkaHeaders;
  }

  public void setPropagationAsKafkaHeaders(
      PropagationAsKafkaHeadersProperties propagationAsKafkaHeaders) {
    this.propagationAsKafkaHeaders = propagationAsKafkaHeaders;
  }

  public Set<String> getAllowedSet() {
    return allowedSet;
  }

  public void setAllowedSet(Set<String> allowedSet) {
    this.allowedSet = allowedSet;
  }

  public Set<String> getAdditionalAllowedSetToLog() {
    return additionalAllowedSetToLog;
  }

  public void setAdditionalAllowedSetToLog(Set<String> additionalAllowedSetToLog) {
    this.additionalAllowedSetToLog = additionalAllowedSetToLog;
  }
}
