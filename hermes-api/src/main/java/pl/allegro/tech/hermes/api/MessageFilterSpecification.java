package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.Objects;

public class MessageFilterSpecification {
  private final String type;
  private final Map<String, Object> spec;

  @JsonCreator
  public MessageFilterSpecification(Map<String, Object> spec) {
    this.spec = spec;
    this.type = getStringValue("type");
  }

  public String getType() {
    return type;
  }

  public String getPath() {
    return getStringValue("path");
  }

  public String getMatcher() {
    return getStringValue("matcher");
  }

  public String getHeader() {
    return getStringValue("header");
  }

  public String getMatchingStrategy() {
    return getStringValue("matchingStrategy");
  }

  @SuppressWarnings("unchecked")
  public <T> T getFieldValue(String key) {
    return (T) spec.get(key);
  }

  public String getStringValue(String key) {
    return getFieldValue(key);
  }

  @JsonValue
  public Object getJsonValue() {
    return spec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageFilterSpecification that = (MessageFilterSpecification) o;
    return Objects.equals(type, that.type) && Objects.equals(spec, that.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, spec);
  }
}
