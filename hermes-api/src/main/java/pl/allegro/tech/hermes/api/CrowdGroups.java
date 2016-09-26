package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrowdGroups {

    private final List<CrowdGroupDescription> crowdGroupDescriptions;

    public CrowdGroups(@JsonProperty("groups") List<CrowdGroupDescription> crowdGroupDescriptions) {
        this.crowdGroupDescriptions = crowdGroupDescriptions;
    }

    public List<CrowdGroupDescription> getCrowdGroupDescriptions() {
        return crowdGroupDescriptions;
    }
}
