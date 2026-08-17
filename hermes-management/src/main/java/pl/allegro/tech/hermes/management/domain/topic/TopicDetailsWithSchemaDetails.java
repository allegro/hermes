package pl.allegro.tech.hermes.management.domain.topic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.List;
import pl.allegro.tech.hermes.api.TopicWithSchema;

public record TopicDetailsWithSchemaDetails(
    @JsonUnwrapped TopicWithSchema topic,
    @JsonInclude(JsonInclude.Include.ALWAYS) Integer schemaVersion,
    @JsonInclude(JsonInclude.Include.ALWAYS) List<Integer> availableSchemaVersions,
    @JsonInclude(JsonInclude.Include.ALWAYS) String schemaSubject) {}
