package pl.allegro.tech.hermes.domain.readiness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import pl.allegro.tech.hermes.api.DatacenterReadiness;

public record DatacenterReadinessList(List<DatacenterReadiness> datacenters) {
  @JsonCreator
  public DatacenterReadinessList(
      @JsonProperty("datacenters") List<DatacenterReadiness> datacenters) {
    this.datacenters = datacenters;
  }
}
