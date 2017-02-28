package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MessageValidationWrapper {
    private final String message;
    private final List<MessageFilterSpecification> filterSpecifications;
    private final Integer schemaVersion;

    @JsonCreator
    public MessageValidationWrapper(@JsonProperty("message") String message,
                                    @JsonProperty("filters") List<MessageFilterSpecification> filtersSpecifications,
                                    @JsonProperty("schemaVersion") @JsonInclude(JsonInclude.Include.NON_NULL) Integer schemaVersion) {
        this.message = message;
        this.filterSpecifications = filtersSpecifications;
        this.schemaVersion = schemaVersion;
    }

    public String getMessage() {
        return message;
    }

    public List<MessageFilterSpecification> getFilterSpecifications() {
        return filterSpecifications;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }
}
