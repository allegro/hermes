package pl.allegro.tech.hermes.domain.readiness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.allegro.tech.hermes.api.DatacenterReadiness;

import java.util.List;

public record DatacenterReadinessList(List<DatacenterReadiness> datacenters) {
    @JsonCreator
    public DatacenterReadinessList(
            @JsonProperty("datacenters") List<DatacenterReadiness> datacenters) {
        this.datacenters = datacenters;
    }
}
