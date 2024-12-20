package pl.allegro.tech.hermes.tracker.management;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrackingUrl(@JsonProperty String name, @JsonProperty String url) {}
