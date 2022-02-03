package pl.allegro.tech.hermes.common.config;

public class MissingConfigPropertyException extends RuntimeException {

    public MissingConfigPropertyException(String propertyName) {
        super(String.format("No config for name: %s", propertyName));
    }
}
