package pl.allegro.tech.hermes.management.domain.clients;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class IframeSource {

  private final String source;

  @JsonCreator
  public IframeSource(@JsonProperty("source") String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IframeSource that = (IframeSource) o;
    return Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source);
  }
}
