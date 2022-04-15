package pl.allegro.tech.hermes.common.config;

public class DuplicateConfigPropertyException extends RuntimeException {

    public DuplicateConfigPropertyException(String propertyName) {
        super(String.format("Duplicate config properties found for property name: %s ", propertyName));
    }
}
