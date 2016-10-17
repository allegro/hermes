package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterValidation {

    public boolean isFiltered() {
        return isFiltered;
    }

    private final boolean isFiltered;

    @JsonCreator
    public FilterValidation(@JsonProperty("filtered") boolean isFiltered) {
        this.isFiltered = isFiltered;
    }
}
