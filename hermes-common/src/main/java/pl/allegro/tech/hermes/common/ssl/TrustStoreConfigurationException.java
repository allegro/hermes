package pl.allegro.tech.hermes.common.ssl;

public class TrustStoreConfigurationException extends RuntimeException {
    public TrustStoreConfigurationException() {
        super("Not found truststore configuration");
    }
}
