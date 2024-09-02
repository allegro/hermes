package pl.allegro.tech.hermes.management.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class MetricsDashboardUrl {

  private final String url;

  @JsonCreator
  public MetricsDashboardUrl(@JsonProperty("url") String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MetricsDashboardUrl that = (MetricsDashboardUrl) o;

    return Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }
}
