package pl.allegro.tech.hermes.management.infrastructure.schema.validator;

public interface SchemaValidator {
    void check(String schema) throws InvalidSchemaException;
}
