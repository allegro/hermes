package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

import pl.allegro.tech.hermes.common.exception.InvalidSchemaException;

public interface SchemaValidator {
    void check(String schema) throws InvalidSchemaException;
}
