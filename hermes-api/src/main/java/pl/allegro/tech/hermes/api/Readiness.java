package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

public class Readiness {
    @NotNull
    private final boolean isReady;

    @JsonCreator
    public Readiness(@JsonProperty("isReady") boolean isReady) {
        this.isReady = isReady;
    }

    @JsonGetter("isReady")
    public boolean isReady() {
        return isReady;
    }
}
