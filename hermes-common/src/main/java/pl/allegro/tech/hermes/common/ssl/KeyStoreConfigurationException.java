package pl.allegro.tech.hermes.common.ssl;

public class KeyStoreConfigurationException extends RuntimeException {
    public KeyStoreConfigurationException() {
        super("Not found keystore configuration");
    }
}
