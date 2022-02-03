package pl.allegro.tech.hermes.common.config;

public class GetConfigPropertyException extends RuntimeException {
    public GetConfigPropertyException(String propertyName) {
        super(String.format("No config for name: %s", propertyName));
    }
}
