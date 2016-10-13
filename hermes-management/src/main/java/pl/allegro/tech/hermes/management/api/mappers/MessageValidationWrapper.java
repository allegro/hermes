package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Optional;

public class MessageValidationWrapper {
    private final String message;
    private final List<MessageFilterSpecification> filterSpecifications;
    private final Optional<Integer> schemaVersion;

    @ConstructorProperties({"message", "filters", "schemaVersion"})
    public MessageValidationWrapper(String message, List<MessageFilterSpecification> filtersSpecifications, @Nullable Integer schemaVersion) {
        this.message = message;
        this.filterSpecifications = filtersSpecifications;
        this.schemaVersion = Optional.ofNullable(schemaVersion);
    }

    public String getMessage() {
        return message;
    }

    public List<MessageFilterSpecification> getFilterSpecifications() {
        return filterSpecifications;
    }

    public Optional<Integer> getSchemaVersion() {
        return schemaVersion;
    }
}